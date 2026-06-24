package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.command.ApproveSalesOrderCommand;
import com.fallguys.salesservice.application.port.inbound.usecase.ApproveSalesOrderUseCase;
import com.fallguys.salesservice.application.port.outbound.model.Executor;
import com.fallguys.salesservice.application.port.outbound.port.AppendSalesOrderStatusHistoryPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.OutboundStockPort;
import com.fallguys.salesservice.application.port.outbound.port.SaveSalesOrderPort;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.CommonErrorCode;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.exception.SalesOrderException;
import com.fallguys.salesservice.domain.model.ActorRef;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.salesorderhistory.ApprovalPayload;
import com.fallguys.salesservice.domain.model.salesorderhistory.SalesOrderStatusHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class ApproveSalesOrderService implements ApproveSalesOrderUseCase {    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

    private final LoadSalesOrderPort loadSalesOrderPort;
    private final SaveSalesOrderPort saveSalesOrderPort;
    private final OutboundStockPort outboundStockPort;
    private final AppendSalesOrderStatusHistoryPort appendHistoryPort;

    /**
     * REQUESTED 상태의 발주를 APPROVED로 전환하고 재고 출고를 기록한다.
     *
     * 흐름:
     * 1) 역할 검증 — ADMIN·HQ_MANAGER·HQ_STAFF만 허용
     * 2) SO 조회
     * 3) 승인일 검증 — approvedDate가 requestedAt 날짜보다 이전이면 거부
     * 4) 도메인 상태 전환 — APPROVED 전환 + saga SENDING
     * 5) 저장 + 승인 이력 기록
     * 6) 출고 이벤트를 outbox에 적재(동일 트랜잭션)
     *
     * 트랜잭션: 쓰기. 상태 전환·저장·이력·outbox 적재가 한 트랜잭션으로 커밋된다.
     * 동기 REST 호출이 아니라 outbox 적재이므로 비즈니스 변경과 메시지가 원자적으로 커밋되고,
     * 커밋 후 릴레이가 broker로 발행한다. 재고 처리 결과는 reply 이벤트로 비동기 수신한다.
     *
     * 예외:
     * - 미허용 역할: ForbiddenException (ER-403, 403)
     * - SO 미존재: ResourceNotFoundException (SO-014, 404)
     * - 승인일 < 요청일: SalesOrderException (SO-007, 400)
     * - REQUESTED 아님: InvalidStatusTransitionException (SO-018, 409)
     */
    @Override
    @Transactional
    public SalesOrder approve(ApproveSalesOrderCommand command) {
        if (!command.role().isHqUser()) {
            throw new ForbiddenException(CommonErrorCode.UNAUTHORIZED);
        }

        SalesOrder order = loadSalesOrderPort.load(command.soCode());

        validateApprovedDate(command.approvedDate(), order);

        Instant now = Instant.now();
        order.approve();

        SalesOrder saved = saveSalesOrderPort.save(order);
        appendHistoryPort.append(SalesOrderStatusHistory.of(
                saved.getCode(), SalesOrderStatus.APPROVED,
                ActorRef.of(command.approvedBy(), command.approverName(), command.approverPosition()),
                new ApprovalPayload(command.approvedDate(), command.carrierType(), command.invoiceNumber()),
                now));

        outboundStockPort.outbound(saved, new Executor(command.approvedBy(), command.approverName()));

        return saved;
    }

    private void validateApprovedDate(LocalDate approvedDate, SalesOrder order) {
        if (order.getRequest() == null) return;
        LocalDate requestedDate = order.getRequest().requestedAt().atZone(BUSINESS_ZONE).toLocalDate();
        if (approvedDate.isBefore(requestedDate)) {
            throw new SalesOrderException(SalesErrorCode.INVALID_APPROVED_DATE,
                    "승인일은 요청일(" + requestedDate + ")보다 이전일 수 없습니다");
        }
    }
}
