package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.command.ApproveSalesOrderCommand;
import com.fallguys.salesservice.application.port.inbound.usecase.ApproveSalesOrderUseCase;
import com.fallguys.salesservice.application.port.outbound.model.Executor;
import com.fallguys.salesservice.application.port.outbound.port.AppendSalesOrderStatusHistoryPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.OutboundStockPort;
import com.fallguys.salesservice.application.port.outbound.port.PendingStatusChangePort;
import com.fallguys.salesservice.application.port.outbound.port.SaveSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.SyncOutboundStockPort;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.CommonErrorCode;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.exception.SalesOrderException;
import com.fallguys.salesservice.domain.model.ActorRef;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.salesorderhistory.ApprovalPayload;
import com.fallguys.salesservice.domain.model.salesorderhistory.PendingStatusChange;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class ApproveSalesOrderService implements ApproveSalesOrderUseCase {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

    private final LoadSalesOrderPort loadSalesOrderPort;
    private final SaveSalesOrderPort saveSalesOrderPort;
    private final OutboundStockPort outboundStockPort;
    private final SyncOutboundStockPort syncOutboundStockPort;
    private final PendingStatusChangePort pendingStatusChangePort;
    private final AppendSalesOrderStatusHistoryPort appendHistoryPort;

    // true면 재고 출고를 동기 REST로 호출(부하 테스트용), false면 outbox 기반 async. 기본 async.
    @Value("${sales.stock.sync-mode:false}")
    private boolean stockSyncMode;

    /**
     * REQUESTED 상태의 발주를 APPROVED로 전환하고 재고 출고를 기록한다.
     *
     * 흐름:
     * 1) 역할 검증 — ADMIN·HQ_MANAGER·HQ_STAFF만 허용
     * 2) SO 조회
     * 3) 라인 검증 — 발주 라인이 1개 이상인지 확인(없으면 승인 거부)
     * 4) 승인일 검증 — approvedDate가 requestedAt 날짜보다 이전이면 거부
     * 5) 도메인 상태 전환 — APPROVED 전환(provisional) + saga SENDING
     * 6) 재고 출고 처리 — 모드에 따라 분기(sales.stock.sync-mode)
     *    - async(기본): 승인 staging 보관 + 출고 이벤트 outbox 적재. reply 성공 시 이력 승격.
     *    - sync(부하 테스트): 동기 REST 출고 호출 → 성공 시 saga 즉시 DONE + 이력 즉시 기록.
     *
     * 트랜잭션: 쓰기. 상태 전환·저장·이력·(outbox 적재 | 동기 호출)이 한 트랜잭션으로 커밋된다.
     * - async: outbox 적재라 비즈니스 변경과 메시지가 원자적으로 커밋되고, 커밋 후 릴레이가
     *   broker로 발행한다. 재고 처리 결과는 reply 이벤트로 비동기 수신한다.
     * - sync: 재고 REST 호출이 트랜잭션 안에서 일어나 호출 동안 DB 트랜잭션이 열려 있다.
     *   호출 실패 시 예외로 전체 롤백된다(부하 테스트용 경로).
     *
     * 예외:
     * - 미허용 역할: ForbiddenException (ER-403, 403)
     * - SO 미존재: ResourceNotFoundException (SO-014, 404)
     * - 발주 라인 없음: SalesOrderException (SO-020, 400)
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

        validateHasLines(order);
        validateApprovedDate(command.approvedDate(), order);

        Instant now = Instant.now();
        order.approve();

        SalesOrder saved = saveSalesOrderPort.save(order);
        PendingStatusChange pending = new PendingStatusChange(
                saved.getCode(), SalesOrderStatus.APPROVED,
                ActorRef.of(command.approvedBy(), command.approverName(), command.approverPosition()),
                new ApprovalPayload(command.approvedDate(), command.carrierType(), command.invoiceNumber()),
                now);

        if (stockSyncMode) {
            // sync 경로(부하 테스트): 같은 트랜잭션 안에서 재고 출고를 동기 REST로 호출한다.
            // 실패 시 예외가 전파돼 상태 전환까지 전부 롤백된다(provisional 미잔존).
            // 성공하면 reply를 기다리지 않고 즉시 saga를 DONE으로 확정하고 이력을 기록한다
            // (CompleteStockSagaService와 동일한 확정 로직을 인라인으로 수행).
            syncOutboundStockPort.outbound(saved);
            saved.markSagaProcessing();
            saved.markSagaDone();
            saveSalesOrderPort.save(saved);
            appendHistoryPort.append(pending.toHistory());
        } else {
            // async 경로(기본): 출고 saga가 DONE으로 확정돼야 이력에 남는다. 지금은 행위자·승인
            // 부가 데이터를 staging에만 보관하고, reply 성공 수신 시 이력으로 승격한다(실패 시 폐기).
            pendingStatusChangePort.save(pending);
            outboundStockPort.outbound(saved, new Executor(command.approvedBy(), command.approverName()));
        }

        return saved;
    }

    private void validateHasLines(SalesOrder order) {
        if (order.getLines() == null || order.getLines().isEmpty()) {
            throw new SalesOrderException(SalesErrorCode.EMPTY_ORDER_LINES);
        }
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
