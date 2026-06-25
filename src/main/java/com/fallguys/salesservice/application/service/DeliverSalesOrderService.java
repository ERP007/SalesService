package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.command.DeliverSalesOrderCommand;
import com.fallguys.salesservice.application.port.inbound.usecase.DeliverSalesOrderUseCase;
import com.fallguys.salesservice.application.port.outbound.model.Executor;
import com.fallguys.salesservice.application.port.outbound.port.AppendSalesOrderStatusHistoryPort;
import com.fallguys.salesservice.application.port.outbound.port.InboundStockPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderStatusHistoryPort;
import com.fallguys.salesservice.application.port.outbound.port.PendingStatusChangePort;
import com.fallguys.salesservice.application.port.outbound.port.SaveSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.SyncInboundStockPort;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.CommonErrorCode;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.exception.SalesOrderException;
import com.fallguys.salesservice.domain.model.ActorRef;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.salesorderhistory.DeliveryPayload;
import com.fallguys.salesservice.domain.model.salesorderhistory.PendingStatusChange;
import com.fallguys.salesservice.domain.model.salesorderhistory.SalesOrderStatusHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    private final SyncInboundStockPort syncInboundStockPort;
    private final PendingStatusChangePort pendingStatusChangePort;
    private final AppendSalesOrderStatusHistoryPort appendHistoryPort;

    // true면 재고 입고를 동기 REST로 호출(부하 테스트용), false면 outbox 기반 async. 기본 async.
    @Value("${sales.stock.sync-mode:false}")
    private boolean stockSyncMode;

    /**
     * APPROVED 상태의 발주를 DELIVERED로 전환하고 재고 입고를 기록한다.
     *
     * 흐름:
     * 1) 역할 검증 — BRANCH_MANAGER, BRANCH_STAFF만 허용
     * 2) SO 존재 확인 (local DB)
     * 3) JWT warehouseCode가 SO의 fromWarehouseCode(발주 지점=입고 창고)와 일치하는지 검증
     * 4) deliveredDate가 출고일(approvedAt) 이전인지 검증
     * 5) 도메인 상태 전환 — DELIVERED 전환(provisional) + saga SENDING
     * 6) 재고 입고 처리 — 모드에 따라 분기(sales.stock.sync-mode)
     *    - async(기본): 배송 staging 보관 + 입고 이벤트 outbox 적재. reply 성공 시 이력 승격.
     *    - sync(부하 테스트): 동기 REST 입고 호출 → 성공 시 saga 즉시 DONE + 이력 즉시 기록.
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
     * - 창고 불일치: ForbiddenException (SO-013, 403)
     * - deliveredDate < 출고일: SalesOrderException (SO-003, 400)
     * - APPROVED 아님: InvalidStatusTransitionException (SO-018, 409)
     */
    @Override
    @Transactional
    public SalesOrder deliver(DeliverSalesOrderCommand command) {
        if (!command.role().isBranchUser()) {
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
        PendingStatusChange pending = new PendingStatusChange(
                saved.getCode(), SalesOrderStatus.DELIVERED,
                ActorRef.of(command.deliveredBy(), command.delivererName(), command.delivererPosition()),
                new DeliveryPayload(command.deliveredDate()), now);

        if (stockSyncMode) {
            // sync 경로(부하 테스트): 같은 트랜잭션 안에서 재고 입고를 동기 REST로 호출한다.
            // 실패 시 예외가 전파돼 상태 전환까지 전부 롤백된다(provisional 미잔존).
            // 성공하면 reply를 기다리지 않고 즉시 saga를 DONE으로 확정하고 이력을 기록한다
            // (CompleteStockSagaService와 동일한 확정 로직을 인라인으로 수행).
            syncInboundStockPort.inbound(saved);
            saved.markSagaProcessing();
            saved.markSagaDone();
            saveSalesOrderPort.save(saved);
            appendHistoryPort.append(pending.toHistory());
        } else {
            // async 경로(기본): 입고 saga가 DONE으로 확정돼야 이력에 남는다. 지금은 staging에만
            // 보관하고, reply 성공 수신 시 이력으로 승격한다(실패 시 폐기).
            pendingStatusChangePort.save(pending);
            inboundStockPort.inbound(saved, new Executor(command.deliveredBy(), command.delivererName()));
        }

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
