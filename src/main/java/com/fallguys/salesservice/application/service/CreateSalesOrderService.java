package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.CreateSalesOrderCommand;
import com.fallguys.salesservice.application.port.inbound.CreateSalesOrderLineCommand;
import com.fallguys.salesservice.application.port.inbound.CreateSalesOrderUseCase;
import com.fallguys.salesservice.application.port.outbound.BranchUserInfo;
import com.fallguys.salesservice.application.port.outbound.GenerateSoCodePort;
import com.fallguys.salesservice.application.port.outbound.ItemInfo;
import com.fallguys.salesservice.application.port.outbound.LoadBranchUserPort;
import com.fallguys.salesservice.application.port.outbound.LoadItemPort;
import com.fallguys.salesservice.application.port.outbound.SaveSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.VerifyWarehousePort;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.exception.SalesOrderException;
import com.fallguys.salesservice.domain.model.SalesOrder;
import com.fallguys.salesservice.domain.model.SalesOrderCreation;
import com.fallguys.salesservice.domain.model.SalesOrderLine;
import com.fallguys.salesservice.domain.model.SalesOrderRequest;
import com.fallguys.salesservice.domain.model.SalesOrderStatus;
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
public class CreateSalesOrderService implements CreateSalesOrderUseCase {

    private final LoadBranchUserPort loadBranchUserPort;
    private final VerifyWarehousePort verifyWarehousePort;
    private final LoadItemPort loadItemPort;
    private final GenerateSoCodePort generateSoCodePort;
    private final SaveSalesOrderPort saveSalesOrderPort;

    /**
     * 발주(SalesOrder)를 생성한다.
     *
     * 흐름:
     * 1) 중복 부품 코드 검증 (local)
     * 2) 도착 희망일 범위 검증 (local) — 오늘 초과 ~ 60일 이내
     * 3) User 서비스 호출 → 사번으로 지점 창고 코드(fromWarehouseCode) 확보
     * 4) [REQUESTED만] 창고 존재 검증 (Inventory 서비스)
     * 5) [REQUESTED만] 부품 존재 확인 및 스냅샷 수집 (Item 서비스)
     * 6) SO 코드 채번 (월별 시퀀스, 비관적 락) — 원격 호출 완료 후 락 획득으로 락 범위 최소화
     * 7) 도메인 객체 생성 및 저장
     *
     * 트랜잭션: 쓰기. SO 코드 채번·저장이 한 트랜잭션으로 묶이며 예외 시 전체 롤백.
     * 외부 서비스 호출(3~5)이 트랜잭션 내에 포함되어 DB 커넥션 점유 시간이 늘어남.
     * 추후 외부 호출을 트랜잭션 진입 전으로 분리하는 리팩토링 고려.
     *
     * 예외:
     * - HQ 계열 또는 미허용 역할: ForbiddenException (SO-05-03, 403)
     * - 중복 부품: SalesOrderException (SO-05-01, 400)
     * - 도착 희망일 범위 초과: SalesOrderException (SO-05-02, 400)
     * - 사번 미존재: ResourceNotFoundException (SO-05-06, 404)
     * - 창고 미존재: ResourceNotFoundException (SO-05-04, 404)
     * - 부품 미존재: ResourceNotFoundException (SO-05-05, 404)
     */
    @Override
    @Transactional
    public SalesOrder create(CreateSalesOrderCommand command) {
        if (command.role() != UserRole.BRANCH_MANAGER && command.role() != UserRole.BRANCH_STAFF) {
            throw new ForbiddenException(SalesErrorCode.UNAUTHORIZED);
        }
        validateNoDuplicateItems(command.lines());
        validateDesiredArrivalDate(command.desiredArrivalDate());

        BranchUserInfo branchUser = loadBranchUserPort.load(command.requestedBy());

        Map<String, ItemInfo> itemMap = null;
        if (command.status() == SalesOrderStatus.REQUESTED) {
            verifyWarehousePort.verify(command.toWarehouseCode());
            List<String> itemCodes = command.lines().stream()
                    .map(CreateSalesOrderLineCommand::itemCode)
                    .toList();
            itemMap = loadItemPort.loadAll(itemCodes);
        }

        String soCode = generateSoCodePort.generate();

        List<SalesOrderLine> lines;
        if (command.status() == SalesOrderStatus.REQUESTED) {
            lines = buildRequestedLines(soCode, command.lines(), itemMap);
        } else {
            lines = buildDraftLines(soCode, command.lines());
        }
        Instant now = Instant.now();

        SalesOrder salesOrder = new SalesOrder(
                soCode,
                branchUser.warehouseCode(),
                command.toWarehouseCode(),
                command.status(),
                command.desiredArrivalDate(),
                command.requestMemo(),
                new SalesOrderCreation(command.requestedBy(), now),
                command.status() == SalesOrderStatus.REQUESTED
                        ? new SalesOrderRequest(command.requestedBy(), now)
                        : null,
                null, null, null, null,
                lines
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

    private List<SalesOrderLine> buildRequestedLines(
            String soCode,
            List<CreateSalesOrderLineCommand> lineCommands,
            Map<String, ItemInfo> itemMap
    ) {
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

    private List<SalesOrderLine> buildDraftLines(String soCode, List<CreateSalesOrderLineCommand> lineCommands) {
        return lineCommands.stream()
                .map(cmd -> new SalesOrderLine(
                        null, soCode, cmd.itemCode(),
                        null, null,
                        cmd.quantity(), null, null, cmd.priority()
                ))
                .toList();
    }
}
