package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.command.RejectSalesOrderCommand;
import com.fallguys.salesservice.application.port.inbound.usecase.RejectSalesOrderUseCase;
import com.fallguys.salesservice.application.port.outbound.port.AppendSalesOrderStatusHistoryPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.SaveSalesOrderPort;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.CommonErrorCode;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.exception.SalesOrderException;
import com.fallguys.salesservice.domain.model.ActorRef;
import com.fallguys.salesservice.domain.model.salesorderhistory.RejectReasonCategory;
import com.fallguys.salesservice.domain.model.salesorderhistory.RejectionPayload;
import com.fallguys.salesservice.domain.model.salesorderhistory.SalesOrderStatusHistory;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RejectSalesOrderService implements RejectSalesOrderUseCase {

    private static final Set<UserRole> ALLOWED_ROLES = EnumSet.of(UserRole.ADMIN, UserRole.HQ_MANAGER);

    private final LoadSalesOrderPort loadSalesOrderPort;
    private final SaveSalesOrderPort saveSalesOrderPort;
    private final AppendSalesOrderStatusHistoryPort appendHistoryPort;

    /**
     * REQUESTED 상태의 발주를 REJECTED로 전환한다.
     *
     * 흐름:
     * 1) 역할 검증 — ADMIN·HQ_MANAGER만 허용
     * 2) OTHER 사유 선택 시 memo 필수 검증
     * 3) SO 조회
     * 4) 도메인 상태 전환(REQUESTED 검증 포함) 및 저장
     *
     * 트랜잭션: 쓰기. 조회·반려·저장이 한 트랜잭션으로 묶이며 예외 시 전체 롤백.
     *
     * 예외:
     * - 미허용 역할: ForbiddenException (ER-403, 403)
     * - OTHER이면서 memo 없음: SalesOrderException (SO-009, 400)
     * - SO 미존재: ResourceNotFoundException (SO-014, 404)
     * - REQUESTED 아님: InvalidStatusTransitionException (SO-018, 409)
     */
    @Override
    @Transactional
    public SalesOrder reject(RejectSalesOrderCommand command) {
        if (!ALLOWED_ROLES.contains(command.role())) {
            throw new ForbiddenException(CommonErrorCode.UNAUTHORIZED);
        }

        if (command.reasonCategory() == RejectReasonCategory.OTHER &&
                (command.memo() == null || command.memo().isBlank())) {
            throw new SalesOrderException(SalesErrorCode.REJECT_MEMO_REQUIRED);
        }

        SalesOrder order = loadSalesOrderPort.load(command.soCode());
        Instant now = Instant.now();
        order.reject();
        SalesOrder saved = saveSalesOrderPort.save(order);
        appendHistoryPort.append(SalesOrderStatusHistory.of(
                saved.getCode(), SalesOrderStatus.REJECTED,
                ActorRef.of(command.rejectedBy(), command.rejectedByName(), command.rejectedByPosition()),
                new RejectionPayload(command.reasonCategory(), command.memo()), now));
        return saved;
    }
}
