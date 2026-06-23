package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.command.DeliverSalesOrderCommand;
import com.fallguys.salesservice.application.port.inbound.usecase.DeliverSalesOrderUseCase;
import com.fallguys.salesservice.application.port.outbound.model.Executor;
import com.fallguys.salesservice.application.port.outbound.port.AppendSalesOrderStatusHistoryPort;
import com.fallguys.salesservice.application.port.outbound.port.InboundStockPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderStatusHistoryPort;
import com.fallguys.salesservice.application.port.outbound.port.SaveSalesOrderPort;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.CommonErrorCode;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.exception.SalesOrderException;
import com.fallguys.salesservice.domain.model.ActorRef;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.salesorderhistory.DeliveryPayload;
import com.fallguys.salesservice.domain.model.salesorderhistory.SalesOrderStatusHistory;
import com.fallguys.salesservice.domain.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class DeliverSalesOrderService implements DeliverSalesOrderUseCase {

    private final LoadSalesOrderPort loadSalesOrderPort;
    private final LoadSalesOrderStatusHistoryPort loadHistoryPort;
    private final SaveSalesOrderPort saveSalesOrderPort;
    private final InboundStockPort inboundStockPort;
    private final AppendSalesOrderStatusHistoryPort appendHistoryPort;

    /**
     * APPROVED 상태의 발주를 DELIVERED로 전환하고 재고 입고를 기록한다.
     *
     * 흐름:
     * 1) 역할 검증 — BRANCH_MANAGER, BRANCH_STAFF만 허용
     * 2) SO 존재 확인 (local DB)
     * 3) JWT warehouseCode가 SO의 fromWarehouseCode(발주 지점=입고 창고)와 일치하는지 검증
     * 4) deliveredDate가 출고일(approvedAt) 이전인지 검증
     * 5) 도메인 상태 전환 — DELIVERED 전환 + saga SENDING
     * 6) 저장 + 배송 이력 기록
     * 7) 입고 이벤트를 outbox에 적재(동일 트랜잭션)
     *
     * 트랜잭션: 쓰기. 상태 전환·저장·이력·outbox 적재가 한 트랜잭션으로 커밋된다.
     * 동기 REST 호출이 아니라 outbox 적재이므로 비즈니스 변경과 메시지가 원자적으로 커밋되고,
     * 커밋 후 릴레이가 broker로 발행한다. 재고 처리 결과는 reply 이벤트로 비동기 수신한다.
     *
     * 예외:
     * - 미허용 역할: ForbiddenException (ER-403, 403)
     * - SO 미존재: ResourceNotFoundException (SO-014, 404)
     * - 창고 불일치: ForbiddenException (SO-013, 403)
     * - deliveredDate < 출고일: SalesOrderException (SO-003, 400)
     * - APPROVED 아님: InvalidStatusTransitionException (SO-018, 409)
     */
    @Override
    @Transactional
    public SalesOrder deliver(DeliverSalesOrderCommand command) {
        if (command.role() != UserRole.BRANCH_MANAGER && command.role() != UserRole.BRANCH_STAFF) {
            throw new ForbiddenException(CommonErrorCode.UNAUTHORIZED);
        }

        SalesOrder order = loadSalesOrderPort.load(command.soCode());

        if (!command.requesterWarehouseCode().equals(order.getFrom().code())) {
            throw new ForbiddenException(SalesErrorCode.SO_FORBIDDEN);
        }

        Instant approvedAt = findApprovedAt(command.soCode());
        if (approvedAt != null) {
            validateDeliveredDate(command.deliveredDate(), approvedAt);
        }

        Instant now = Instant.now();
        order.deliver();

        SalesOrder saved = saveSalesOrderPort.save(order);
        appendHistoryPort.append(SalesOrderStatusHistory.of(
                saved.getCode(), SalesOrderStatus.DELIVERED,
                ActorRef.of(command.deliveredBy(), command.delivererName(), command.delivererPosition()),
                new DeliveryPayload(command.deliveredDate()), now));

        inboundStockPort.inbound(saved, new Executor(command.deliveredBy(), command.delivererName()));

        return saved;
    }

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

    // 출고일(approvedAt)은 상태 변경 이력의 APPROVED 행 created_at에서 가져온다(없으면 검증 생략).
    private Instant findApprovedAt(String soCode) {
        return loadHistoryPort.findLatestBySoCodeAndStatus(soCode, SalesOrderStatus.APPROVED)
                .map(SalesOrderStatusHistory::createdAt)
                .orElse(null);
    }

    private void validateDeliveredDate(LocalDate deliveredDate, Instant approvedAt) {
        LocalDate approvedDate = approvedAt.atZone(BUSINESS_ZONE).toLocalDate();
        if (deliveredDate.isBefore(approvedDate)) {
            throw new SalesOrderException(SalesErrorCode.INVALID_DELIVERED_DATE,
                    "도착일은 출고일(" + approvedDate + ")보다 이전일 수 없습니다");
        }
    }
}
