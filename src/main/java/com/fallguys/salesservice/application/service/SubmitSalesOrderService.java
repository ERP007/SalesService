package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.CreateSalesOrderLineCommand;
import com.fallguys.salesservice.application.port.inbound.SubmitSalesOrderCommand;
import com.fallguys.salesservice.application.port.inbound.SubmitSalesOrderUseCase;
import com.fallguys.salesservice.application.port.outbound.BranchUserInfo;
import com.fallguys.salesservice.application.port.outbound.ItemInfo;
import com.fallguys.salesservice.application.port.outbound.LoadBranchUserPort;
import com.fallguys.salesservice.application.port.outbound.LoadItemPort;
import com.fallguys.salesservice.application.port.outbound.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.SaveSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.VerifyWarehousePort;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.exception.SalesOrderException;
import com.fallguys.salesservice.domain.model.SalesOrder;
import com.fallguys.salesservice.domain.model.SalesOrderLine;
import com.fallguys.salesservice.domain.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SubmitSalesOrderService implements SubmitSalesOrderUseCase {

    private final LoadSalesOrderPort loadSalesOrderPort;
    private final LoadBranchUserPort loadBranchUserPort;
    private final VerifyWarehousePort verifyWarehousePort;
    private final LoadItemPort loadItemPort;
    private final SaveSalesOrderPort saveSalesOrderPort;

    /**
     * DRAFT 상태의 발주를 REQUESTED로 전환한다.
     *
     * 흐름:
     * 1) SO 존재 확인 (local DB)
     * 2) DRAFT 상태 검증 (local)
     * 3) 중복 부품 코드 검증 (local)
     * 4) 도착 희망일 범위 검증 (local) — 오늘 초과 ~ 60일 이내
     * 5) User 서비스 호출 → 사번으로 지점 창고 코드 확보 후 SO 소유 지점과 일치 검증
     * 6) 창고 존재 검증 (Inventory 서비스)
     * 7) 부품 존재 확인 및 스냅샷 수집 (Item 서비스)
     * 8) 도메인 상태 전환 및 저장
     *
     * 트랜잭션: 쓰기. 저장이 한 트랜잭션으로 묶이며 예외 시 전체 롤백.
     * 외부 서비스 호출(5~7)이 트랜잭션 내에 포함되어 DB 커넥션 점유 시간이 늘어남.
     * 추후 외부 호출을 트랜잭션 진입 전으로 분리하는 리팩토링 고려.
     *
     * 예외:
     * - HQ 계열 또는 미허용 역할: ForbiddenException (SO-05-03, 403)
     * - SO 미존재: ResourceNotFoundException (SO-06-01, 404)
     * - DRAFT 아님: InvalidStatusTransitionException (SO-05-07, 409)
     * - 중복 부품: SalesOrderException (SO-05-01, 400)
     * - 도착 희망일 범위 초과: SalesOrderException (SO-05-02, 400)
     * - 사번 미존재: ResourceNotFoundException (SO-05-06, 404)
     * - SO 소유 지점 불일치: ForbiddenException (SO-06-02, 403)
     * - 창고 미존재: ResourceNotFoundException (SO-05-04, 404)
     * - 부품 미존재: ResourceNotFoundException (SO-05-05, 404)
     */
    @Override
    @Transactional
    public SalesOrder submit(SubmitSalesOrderCommand command) {
        if (command.role() != UserRole.BRANCH_MANAGER && command.role() != UserRole.BRANCH_STAFF) {
            throw new ForbiddenException(SalesErrorCode.UNAUTHORIZED);
        }
        SalesOrder salesOrder = loadSalesOrderPort.load(command.soCode());

        validateNoDuplicateItems(command.lines());
        validateDesiredArrivalDate(command.desiredArrivalDate());

        BranchUserInfo branchUser = loadBranchUserPort.load(command.requestedBy());
        if (!branchUser.warehouseCode().equals(salesOrder.getFromWarehouseCode())) {
            throw new ForbiddenException(SalesErrorCode.SO_FORBIDDEN);
        }

        verifyWarehousePort.verify(command.toWarehouseCode());

        List<String> itemCodes = command.lines().stream()
                .map(CreateSalesOrderLineCommand::itemCode)
                .toList();
        Map<String, ItemInfo> itemMap = loadItemPort.loadAll(itemCodes);

        List<SalesOrderLine> lines = buildLines(salesOrder.getCode(), command.lines(), itemMap);
        salesOrder.submitRequest(
                command.requestedBy(), Instant.now(),
                command.toWarehouseCode(), command.desiredArrivalDate(),
                command.requestMemo(), lines
        );

        return saveSalesOrderPort.save(salesOrder);
    }

    private void validateNoDuplicateItems(List<CreateSalesOrderLineCommand> lines) {
        Set<String> seen = new HashSet<>();
        for (CreateSalesOrderLineCommand line : lines) {
            if (!seen.add(line.itemCode())) {
                throw new SalesOrderException(SalesErrorCode.DUPLICATE_ITEM,
                        "부품 코드 " + line.itemCode() + "이(가) 중복되었습니다");
            }
        }
    }

    private void validateDesiredArrivalDate(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (!date.isAfter(today)) {
            throw new SalesOrderException(SalesErrorCode.INVALID_DESIRED_ARRIVAL_DATE,
                    "도착 희망일은 오늘 이후여야 합니다");
        }
        if (date.isAfter(today.plusDays(60))) {
            throw new SalesOrderException(SalesErrorCode.INVALID_DESIRED_ARRIVAL_DATE,
                    "도착 희망일은 오늘로부터 60일 이내여야 합니다");
        }
    }

    private List<SalesOrderLine> buildLines(String soCode, List<CreateSalesOrderLineCommand> lineCommands,
                                            Map<String, ItemInfo> itemMap) {
        return lineCommands.stream()
                .map(cmd -> {
                    ItemInfo item = itemMap.get(cmd.itemCode());
                    return new SalesOrderLine(
                            null, soCode, cmd.itemCode(),
                            item.itemName(), item.unit(),
                            cmd.quantity(), null, null, cmd.priority()
                    );
                })
                .toList();
    }
}
