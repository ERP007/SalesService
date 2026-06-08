package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.ApproveSalesOrderCommand;
import com.fallguys.salesservice.application.port.inbound.ApproveSalesOrderUseCase;
import com.fallguys.salesservice.application.port.outbound.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.OutboundStockPort;
import com.fallguys.salesservice.application.port.outbound.SaveSalesOrderPort;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.exception.SalesOrderException;
import com.fallguys.salesservice.domain.model.SalesOrder;
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
     * 트랜잭션: 쓰기. 재고 서비스 호출은 트랜잭션 경계 밖(외부 호출).
     * 재고 호출 실패 시 트랜잭션이 롤백되어 DB 변경도 취소된다.
     *
     * 예외:
     * - 미허용 역할: ForbiddenException (SO-05-03, 403)
     * - 송장 번호 중복: SalesOrderException (SO-05-12, 400)
     * - SO 미존재: ResourceNotFoundException (SO-06-01, 404)
     * - 승인일 < 요청일: SalesOrderException (SO-05-11, 400)
     * - REQUESTED 아님: InvalidStatusTransitionException (SO-05-07, 409)
     * - 재고 출고 실패: SO-07-05~08 또는 ExternalServiceException (SO-07-04, 502)
     */
    @Override
    @Transactional
    public SalesOrder approve(ApproveSalesOrderCommand command) {
        if (!ALLOWED_ROLES.contains(command.role())) {
            throw new ForbiddenException(SalesErrorCode.UNAUTHORIZED);
        }

        if (command.invoiceNumber() != null &&
                loadSalesOrderPort.existsByInvoiceNumber(command.invoiceNumber())) {
            throw new SalesOrderException(SalesErrorCode.DUPLICATE_INVOICE_NUMBER);
        }

        SalesOrder order = loadSalesOrderPort.load(command.soCode());

        validateApprovedDate(command.approvedDate(), order);

        order.approve(command.approvedBy(), Instant.now(), command.approvedDate(),
                command.carrierType(), command.invoiceNumber());

        outboundStockPort.outbound(order);

        return saveSalesOrderPort.save(order);
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
