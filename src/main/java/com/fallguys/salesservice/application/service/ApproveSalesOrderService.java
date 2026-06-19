package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.command.ApproveSalesOrderCommand;
import com.fallguys.salesservice.application.port.inbound.usecase.ApproveSalesOrderUseCase;
import com.fallguys.salesservice.application.port.outbound.port.AppendSalesOrderStatusHistoryPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.OutboundStockPort;
import com.fallguys.salesservice.application.port.outbound.port.SaveSalesOrderPort;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.CommonErrorCode;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.exception.SalesOrderException;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.salesorderhistory.ApprovalPayload;
import com.fallguys.salesservice.domain.model.salesorderhistory.SalesOrderStatusHistory;
import com.fallguys.salesservice.domain.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ApproveSalesOrderService implements ApproveSalesOrderUseCase {

    private static final Set<UserRole> ALLOWED_ROLES = EnumSet.of(
            UserRole.ADMIN, UserRole.HQ_MANAGER, UserRole.HQ_STAFF
    );
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

    private final LoadSalesOrderPort loadSalesOrderPort;
    private final SaveSalesOrderPort saveSalesOrderPort;
    private final OutboundStockPort outboundStockPort;
    private final AppendSalesOrderStatusHistoryPort appendHistoryPort;

    /**
     * REQUESTED 상태의 발주를 APPROVED로 전환하고 재고 출고를 기록한다.
     *
     * 흐름:
     * 1) 역할 검증 — ADMIN·HQ_MANAGER·HQ_STAFF만 허용
     * 2) 송장 번호 중복 검증 — invoiceNumber가 null이 아닐 때만 확인
     * 3) SO 조회
     * 4) 승인일 검증 — approvedDate가 requestedAt 날짜보다 이전이면 거부
     * 5) 도메인 상태 전환 — 라인 approvedQuantity 확정 및 APPROVED 전환
     * 6) 재고 서비스 출고 호출
     * 7) 저장
     *
     * 트랜잭션: 쓰기. DB 저장 후 재고 서비스 출고를 호출한다.
     * 재고 호출 실패 시 SO는 APPROVED 상태로 남고 출고만 미처리된다(재시도 가능).
     * 반대 순서(출고 후 저장 실패)보다 정합성 측면에서 낫다.
     *
     * 예외:
     * - 미허용 역할: ForbiddenException (ER-403, 403)
     * - 송장 번호 중복: SalesOrderException (SO-006, 400)
     * - SO 미존재: ResourceNotFoundException (SO-014, 404)
     * - 승인일 < 요청일: SalesOrderException (SO-007, 400)
     * - REQUESTED 아님: InvalidStatusTransitionException (SO-018, 409)
     * - 재고 출고 실패: SalesOrderException (SO-012, 400) 또는 ExternalServiceException (ER-502, 502)
     */
    @Override
    @Transactional
    public SalesOrder approve(ApproveSalesOrderCommand command) {
        if (!ALLOWED_ROLES.contains(command.role())) {
            throw new ForbiddenException(CommonErrorCode.UNAUTHORIZED);
        }

        if (command.invoiceNumber() != null &&
                loadSalesOrderPort.existsByInvoiceNumber(command.invoiceNumber())) {
            throw new SalesOrderException(SalesErrorCode.DUPLICATE_INVOICE_NUMBER);
        }

        SalesOrder order = loadSalesOrderPort.load(command.soCode());

        validateApprovedDate(command.approvedDate(), order);

        Instant now = Instant.now();
        order.approve(command.approvedBy(), now, command.approvedDate(),
                command.carrierType(), command.invoiceNumber());

        SalesOrder saved = saveSalesOrderPort.save(order);
        appendHistoryPort.append(SalesOrderStatusHistory.of(
                saved.getCode(), SalesOrderStatus.APPROVED, command.approvedBy(),
                new ApprovalPayload(command.approvedDate(), command.carrierType(), command.invoiceNumber()),
                now));

        outboundStockPort.outbound(saved);

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
