package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.command.CreateSalesOrderCommand;
import com.fallguys.salesservice.application.port.inbound.command.CreateSalesOrderLineCommand;
import com.fallguys.salesservice.application.port.inbound.usecase.CreateSalesOrderUseCase;
import com.fallguys.salesservice.application.port.outbound.port.AppendSalesOrderStatusHistoryPort;
import com.fallguys.salesservice.application.port.outbound.port.GenerateSoCodePort;
import com.fallguys.salesservice.application.port.outbound.model.ItemInfo;
import com.fallguys.salesservice.application.port.outbound.port.LoadItemPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadWarehousePort;
import com.fallguys.salesservice.application.port.outbound.port.SaveSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.VerifyWarehousePort;
import com.fallguys.salesservice.domain.model.ActorRef;
import com.fallguys.salesservice.domain.model.WarehouseRef;
import com.fallguys.salesservice.domain.model.salesorderhistory.SalesOrderStatusHistory;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.CommonErrorCode;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.exception.SalesOrderException;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorderline.SalesOrderLine;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
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

    private final VerifyWarehousePort verifyWarehousePort;
    private final LoadWarehousePort loadWarehousePort;
    private final LoadItemPort loadItemPort;
    private final GenerateSoCodePort generateSoCodePort;
    private final SaveSalesOrderPort saveSalesOrderPort;
    private final AppendSalesOrderStatusHistoryPort appendHistoryPort;

    /**
     * 발주(SalesOrder)를 생성한다.
     *
     * 흐름:
     * 1) 중복 부품 코드 검증 (local)
     * 2) 도착 희망일 범위 검증 (local) — 오늘 초과 ~ 60일 이내
     * 3) User 서비스 호출 → 사번으로 지점 창고 코드(fromWarehouseCode) 확보
     * 4) [REQUESTED만] 지점 창고(fromWarehouseCode) 활성 검증 (Inventory 서비스)
     * 4-1) [REQUESTED만] 본사 창고(toWarehouseCode) 활성 검증 (Inventory 서비스)
     * 5) [REQUESTED만] 부품 존재 확인 및 스냅샷 수집 (Item 서비스)
     * 6) SO 코드 채번 (월별 시퀀스, 비관적 락) — 원격 호출 완료 후 락 획득으로 락 범위 최소화
     * 7) 도메인 객체 생성 및 저장
     *
     * 트랜잭션: 쓰기. SO 코드 채번·저장이 한 트랜잭션으로 묶이며 예외 시 전체 롤백.
     * 외부 서비스 호출(3~5)이 트랜잭션 내에 포함되어 DB 커넥션 점유 시간이 늘어남.
     * 추후 외부 호출을 트랜잭션 진입 전으로 분리하는 리팩토링 고려.
     *
     * 예외:
     * - HQ 계열 또는 미허용 역할: ForbiddenException (ER-403, 403)
     * - 중복 부품: SalesOrderException (SO-002, 400)
     * - 도착 희망일 범위 초과: SalesOrderException (SO-003, 400)
     * - 사번 미존재: ResourceNotFoundException (SO-017, 404)
     * - 창고 미존재: ResourceNotFoundException (SO-015, 404)
     * - 창고 비활성: SalesOrderException (SO-004, 400)
     * - 부품 미존재: ResourceNotFoundException (SO-016, 404)
     */
    @Override
    @Transactional
    public SalesOrder create(CreateSalesOrderCommand command) {
        if (!command.role().isBranchUser()) {
            throw new ForbiddenException(CommonErrorCode.UNAUTHORIZED);
        }
        validateNoDuplicateItems(command.lines());
        validateDesiredArrivalDate(command.desiredArrivalDate());

        Map<String, ItemInfo> itemMap = null;
        if (command.status() == SalesOrderStatus.REQUESTED) {
            verifyWarehousePort.verify(command.fromWarehouseCode());
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

        // 행위자(createdBy)는 누가 만들었는지 불변 사실이므로 DRAFT부터 name/position을 박제해
        // 이력에 남긴다. 창고명은 변동 가능하므로 확정(REQUESTED) 시점에만 박제하고 DRAFT는 code만 보관.
        ActorRef createdBy = ActorRef.of(
                command.requestedBy(), command.requesterName(), command.requesterPosition());
        WarehouseRef from;
        WarehouseRef to;
        if (command.status() == SalesOrderStatus.REQUESTED) {
            from = WarehouseRef.of(command.fromWarehouseCode(),
                    loadWarehousePort.load(command.fromWarehouseCode()).warehouseName());
            to = WarehouseRef.of(command.toWarehouseCode(),
                    loadWarehousePort.load(command.toWarehouseCode()).warehouseName());
        } else {
            from = WarehouseRef.codeOnly(command.fromWarehouseCode());
            to = WarehouseRef.codeOnly(command.toWarehouseCode());
        }

        SalesOrder salesOrder = SalesOrder.create(
                soCode, from, to, command.status(),
                command.desiredArrivalDate(), command.requestMemo(),
                createdBy, now, lines
        );

        SalesOrder saved = saveSalesOrderPort.save(salesOrder);
        appendHistoryPort.append(SalesOrderStatusHistory.of(
                saved.getCode(), command.status(), createdBy, now));
        return saved;
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
                            cmd.quantity(), cmd.priority()
                    );
                })
                .toList();
    }

    private List<SalesOrderLine> buildDraftLines(String soCode, List<CreateSalesOrderLineCommand> lineCommands) {
        return lineCommands.stream()
                .map(cmd -> new SalesOrderLine(
                        null, soCode, cmd.itemCode(),
                        null, null,
                        cmd.quantity(), cmd.priority()
                ))
                .toList();
    }
}
