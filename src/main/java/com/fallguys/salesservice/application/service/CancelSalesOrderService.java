package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.command.CancelSalesOrderCommand;
import com.fallguys.salesservice.application.port.inbound.usecase.CancelSalesOrderUseCase;
import com.fallguys.salesservice.application.port.outbound.port.AppendSalesOrderStatusHistoryPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.SaveSalesOrderPort;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.CommonErrorCode;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.model.ActorRef;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.salesorderhistory.CancellationPayload;
import com.fallguys.salesservice.domain.model.salesorderhistory.SalesOrderStatusHistory;
import com.fallguys.salesservice.domain.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CancelSalesOrderService implements CancelSalesOrderUseCase {

    private final LoadSalesOrderPort loadSalesOrderPort;
    private final SaveSalesOrderPort saveSalesOrderPort;
    private final AppendSalesOrderStatusHistoryPort appendHistoryPort;

    /**
     * REQUESTED 상태의 발주를 CANCELED로 전환한다.
     *
     * 흐름:
     * 1) 역할 검증 — BRANCH_MANAGER, BRANCH_STAFF만 허용
     * 2) SO 존재 확인 (local DB)
     * 3) User 서비스 호출 → 요청자의 소속 창고가 SO의 fromWarehouseCode와 일치하는지 검증
     * 4) BRANCH_STAFF이면 자신이 요청(requestedBy)한 발주인지 추가 검증
     * 5) 도메인 상태 전환(상태 검증 포함) 및 저장
     *
     * 트랜잭션: 쓰기. 저장이 한 트랜잭션으로 묶이며 예외 시 전체 롤백.
     *
     * 예외:
     * - HQ 계열 또는 미허용 역할: ForbiddenException (ER-403, 403)
     * - SO 미존재: ResourceNotFoundException (SO-014, 404)
     * - 소속 창고 불일치: ForbiddenException (SO-013, 403)
     * - BRANCH_STAFF가 타인 발주 취소 시도: ForbiddenException (SO-013, 403)
     * - REQUESTED 아님: InvalidStatusTransitionException (SO-018, 409)
     */
    @Override
    @Transactional
    public SalesOrder cancel(CancelSalesOrderCommand command) {
        if (command.role() != UserRole.BRANCH_MANAGER && command.role() != UserRole.BRANCH_STAFF) {
            throw new ForbiddenException(CommonErrorCode.UNAUTHORIZED);
        }

        SalesOrder salesOrder = loadSalesOrderPort.load(command.soCode());

        if (!command.requesterWarehouseCode().equals(salesOrder.getFrom().code())) {
            throw new ForbiddenException(SalesErrorCode.SO_FORBIDDEN);
        }

        if (command.role() == UserRole.BRANCH_STAFF &&
                salesOrder.getStatus() == SalesOrderStatus.REQUESTED) {
            String requestedBy = salesOrder.getRequest().requestedBy().code();
            if (requestedBy == null || !requestedBy.equals(command.canceledBy())) {
                throw new ForbiddenException(SalesErrorCode.SO_FORBIDDEN);
            }
        }

        Instant now = Instant.now();
        salesOrder.cancel();
        SalesOrder saved = saveSalesOrderPort.save(salesOrder);
        appendHistoryPort.append(SalesOrderStatusHistory.of(
                saved.getCode(), SalesOrderStatus.CANCELED,
                ActorRef.of(command.canceledBy(), command.canceledByName(), command.canceledByPosition()),
                new CancellationPayload(command.reason()), now));
        return saved;
    }
}
