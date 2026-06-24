package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.command.CreateSalesOrderLineCommand;
import com.fallguys.salesservice.application.port.inbound.command.SubmitSalesOrderCommand;
import com.fallguys.salesservice.application.port.inbound.usecase.SubmitSalesOrderUseCase;
import com.fallguys.salesservice.application.port.outbound.model.ItemInfo;
import com.fallguys.salesservice.application.port.outbound.port.AppendSalesOrderStatusHistoryPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadItemPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadWarehousePort;
import com.fallguys.salesservice.application.port.outbound.port.SaveSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.VerifyWarehousePort;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.model.ActorRef;
import com.fallguys.salesservice.domain.model.WarehouseRef;
import com.fallguys.salesservice.domain.exception.CommonErrorCode;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.exception.SalesOrderException;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.salesorderhistory.SalesOrderStatusHistory;
import com.fallguys.salesservice.domain.model.salesorderline.SalesOrderLine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SubmitSalesOrderService implements SubmitSalesOrderUseCase {

    private final LoadSalesOrderPort loadSalesOrderPort;
    private final VerifyWarehousePort verifyWarehousePort;
    private final LoadWarehousePort loadWarehousePort;
    private final LoadItemPort loadItemPort;
    private final SaveSalesOrderPort saveSalesOrderPort;
    private final AppendSalesOrderStatusHistoryPort appendHistoryPort;

    /**
     * DRAFT 상태의 발주를 REQUESTED로 전환한다.
     *
     * 흐름:
     * 1) SO 존재 확인 (local DB)
     * 2) DRAFT 상태 검증 (local)
     * 3) 중복 부품 코드 검증 (local)
     * 5) 요청자 창고 코드(JWT)와 SO 소유 지점 일치 검증 (local)
     * 6) 지점 창고(fromWarehouseCode) 활성 검증 (Inventory 서비스)
     * 6-1) 본사 창고(toWarehouseCode) 활성 검증 (Inventory 서비스)
     * 7) 부품 존재 확인 및 스냅샷 수집 (Item 서비스)
     * 8) 도메인 상태 전환 및 저장
     *
     * 트랜잭션: 쓰기. 저장이 한 트랜잭션으로 묶이며 예외 시 전체 롤백.
     * 외부 서비스 호출(5~7)이 트랜잭션 내에 포함되어 DB 커넥션 점유 시간이 늘어남.
     * 추후 외부 호출을 트랜잭션 진입 전으로 분리하는 리팩토링 고려.
     *
     * 예외:
     * - HQ 계열 또는 미허용 역할: ForbiddenException (ER-403, 403)
     * - SO 미존재: ResourceNotFoundException (SO-014, 404)
     * - DRAFT 아님: InvalidStatusTransitionException (SO-018, 409)
     * - 중복 부품: SalesOrderException (SO-002, 400)
     * - SO 소유 지점 불일치: ForbiddenException (SO-013, 403)
     * - 창고 미존재: ResourceNotFoundException (SO-015, 404)
     * - 창고 비활성: SalesOrderException (SO-004, 400)
     * - 부품 미존재: ResourceNotFoundException (SO-016, 404)
     */
    @Override
    @Transactional
    public SalesOrder submit(SubmitSalesOrderCommand command) {
        if (!command.role().isBranchUser()) {
            throw new ForbiddenException(CommonErrorCode.UNAUTHORIZED);
        }
        SalesOrder salesOrder = loadSalesOrderPort.load(command.soCode());

        validateLinesNotEmpty(command.lines());
        validateNoDuplicateItems(command.lines());

        if (!command.requesterWarehouseCode().equals(salesOrder.getFrom().code())) {
            throw new ForbiddenException(SalesErrorCode.SO_FORBIDDEN);
        }

        verifyWarehousePort.verify(salesOrder.getFrom().code());
        verifyWarehousePort.verify(command.toWarehouseCode());

        List<String> itemCodes = command.lines().stream()
                .map(CreateSalesOrderLineCommand::itemCode)
                .toList();
        Map<String, ItemInfo> itemMap = loadItemPort.loadAll(itemCodes);

        List<SalesOrderLine> lines = buildLines(salesOrder.getCode(), command.lines(), itemMap);
        Instant now = Instant.now();

        // 제출은 확정이므로 from·to 창고명과 요청자 name/position을 박제한다.
        ActorRef requestedBy = ActorRef.of(
                command.requestedBy(), command.requesterName(), command.requesterPosition());
        WarehouseRef from = WarehouseRef.of(salesOrder.getFrom().code(),
                loadWarehousePort.load(salesOrder.getFrom().code()).warehouseName());
        WarehouseRef to = WarehouseRef.of(command.toWarehouseCode(),
                loadWarehousePort.load(command.toWarehouseCode()).warehouseName());
        salesOrder.submitRequest(
                requestedBy, now, from, to,
                command.requestMemo(), lines
        );

        SalesOrder saved = saveSalesOrderPort.save(salesOrder);
        appendHistoryPort.append(SalesOrderStatusHistory.of(
                saved.getCode(), SalesOrderStatus.REQUESTED, requestedBy, now));
        return saved;
    }

    private void validateLinesNotEmpty(List<CreateSalesOrderLineCommand> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new SalesOrderException(SalesErrorCode.INVALID_REQUEST,
                    "발주 품목은 1개 이상이어야 합니다");
        }
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

    private List<SalesOrderLine> buildLines(String soCode, List<CreateSalesOrderLineCommand> lineCommands,
                                            Map<String, ItemInfo> itemMap) {
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
}
