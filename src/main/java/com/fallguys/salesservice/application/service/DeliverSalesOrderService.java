package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.command.DeliverSalesOrderCommand;
import com.fallguys.salesservice.application.port.inbound.usecase.DeliverSalesOrderUseCase;
import com.fallguys.salesservice.application.port.outbound.port.AppendSalesOrderStatusHistoryPort;
import com.fallguys.salesservice.application.port.outbound.port.InboundStockPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.SaveSalesOrderPort;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.CommonErrorCode;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.exception.SalesOrderException;
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
     * 5) 도메인 상태 전환 — 각 라인 deliveredQuantity 확정 및 DELIVERED 전환
     * 6) 저장
     * 7) 재고 서비스 입고 호출
     *
     * 트랜잭션: 쓰기. 재고 서비스 호출은 트랜잭션 경계 밖(외부 호출).
     * 재고 호출 실패 시 트랜잭션이 롤백되어 DB 변경도 취소된다.
     *
     * 예외:
     * - 미허용 역할: ForbiddenException (ER-403, 403)
     * - SO 미존재: ResourceNotFoundException (SO-014, 404)
     * - 창고 불일치: ForbiddenException (SO-013, 403)
     * - deliveredDate < 출고일: SalesOrderException (SO-003, 400)
     * - APPROVED 아님: InvalidStatusTransitionException (SO-018, 409)
     * - 재고 서비스 실패: ExternalServiceException (ER-502, 502)
     */
    @Override
    @Transactional
    public SalesOrder deliver(DeliverSalesOrderCommand command) {
        if (command.role() != UserRole.BRANCH_MANAGER && command.role() != UserRole.BRANCH_STAFF) {
            throw new ForbiddenException(CommonErrorCode.UNAUTHORIZED);
        }

        SalesOrder order = loadSalesOrderPort.load(command.soCode());

        if (!command.requesterWarehouseCode().equals(order.getFromWarehouseCode())) {
            throw new ForbiddenException(SalesErrorCode.SO_FORBIDDEN);
        }

        if (order.getApproval() != null) {
            validateDeliveredDate(command.deliveredDate(), order);
        }

        Instant now = Instant.now();
        order.deliver(command.deliveredBy(), command.deliveredDate(), now);

        SalesOrder saved = saveSalesOrderPort.save(order);
        appendHistoryPort.append(SalesOrderStatusHistory.of(
                saved.getCode(), SalesOrderStatus.DELIVERED, command.deliveredBy(),
                new DeliveryPayload(command.deliveredDate(), null, null), now));

        inboundStockPort.inbound(saved);

        return saved;
    }

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

    private void validateDeliveredDate(LocalDate deliveredDate, SalesOrder order) {
        LocalDate approvedDate = order.getApproval().approvedAt().atZone(BUSINESS_ZONE).toLocalDate();
        if (deliveredDate.isBefore(approvedDate)) {
            throw new SalesOrderException(SalesErrorCode.INVALID_DELIVERED_DATE,
                    "도착일은 출고일(" + approvedDate + ")보다 이전일 수 없습니다");
        }
    }
}
