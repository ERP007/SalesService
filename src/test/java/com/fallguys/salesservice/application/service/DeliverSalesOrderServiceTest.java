package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.command.DeliverSalesOrderCommand;
import com.fallguys.salesservice.application.port.outbound.port.InboundStockPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderStatusHistoryPort;
import com.fallguys.salesservice.application.port.outbound.port.PendingStatusChangePort;
import com.fallguys.salesservice.application.port.outbound.port.SaveSalesOrderPort;
import com.fallguys.salesservice.domain.exception.CommonErrorCode;
import com.fallguys.salesservice.domain.exception.ExternalServiceException;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.InvalidStatusTransitionException;
import com.fallguys.salesservice.domain.exception.ResourceNotFoundException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.exception.SalesOrderException;
import com.fallguys.salesservice.domain.model.ActorRef;
import com.fallguys.salesservice.domain.model.UserRole;
import com.fallguys.salesservice.domain.model.WarehouseRef;
import com.fallguys.salesservice.domain.model.salesorder.SagaStatus;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderCreation;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderRequest;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.salesorderhistory.ApprovalPayload;
import com.fallguys.salesservice.domain.model.salesorderhistory.CarrierType;
import com.fallguys.salesservice.domain.model.salesorderhistory.DeliveryPayload;
import com.fallguys.salesservice.domain.model.salesorderhistory.SalesOrderStatusHistory;
import com.fallguys.salesservice.domain.model.salesorderline.Priority;
import com.fallguys.salesservice.domain.model.salesorderline.SalesOrderLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeliverSalesOrderServiceTest {

    @Mock LoadSalesOrderPort loadSalesOrderPort;
    @Mock LoadSalesOrderStatusHistoryPort loadHistoryPort;
    @Mock SaveSalesOrderPort saveSalesOrderPort;
    @Mock PendingStatusChangePort pendingStatusChangePort;
    @Mock InboundStockPort inboundStockPort;

    @InjectMocks
    DeliverSalesOrderService service;

    private static final String SO_CODE = "SO-2026-06-0001";
    private static final String USER_CODE = "branch001";
    private static final String FROM_WAREHOUSE = "WH-BRANCH-01";
    private static final String OTHER_WAREHOUSE = "WH-BRANCH-99";
    private static final Instant APPROVED_AT = Instant.parse("2026-06-01T00:00:00Z");
    private static final LocalDate VALID_DELIVERED_DATE = LocalDate.of(2026, 6, 3);
    private static final LocalDate BEFORE_APPROVED_DATE = LocalDate.of(2026, 5, 31);

    private static final ActorRef ACTOR = ActorRef.of(USER_CODE, "정유진", "지점 담당");
    private static final ActorRef HQ_ACTOR = ActorRef.of("hq001", "강지석", "본사 매니저");

    @BeforeEach
    void setUp() {
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(approvedOrder());
        given(loadHistoryPort.findLatestBySoCodeAndStatus(SO_CODE, SalesOrderStatus.APPROVED))
                .willReturn(Optional.of(SalesOrderStatusHistory.of(
                        SO_CODE, SalesOrderStatus.APPROVED, HQ_ACTOR,
                        new ApprovalPayload(LocalDate.of(2026, 6, 1), CarrierType.VEHICLE, "INV-2026-001"), APPROVED_AT)));
        given(saveSalesOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));
        willDoNothing().given(inboundStockPort).inbound(any(), any());
    }

    @Test
    void MANAGER_도착_확인_성공() {
        SalesOrder result = service.deliver(command(UserRole.BRANCH_MANAGER));

        assertThat(result.getStatus()).isEqualTo(SalesOrderStatus.DELIVERED);
        then(pendingStatusChangePort).should().save(argThat(p ->
                p.status() == SalesOrderStatus.DELIVERED &&
                p.actor().code().equals(USER_CODE) &&
                p.payload() instanceof DeliveryPayload dp &&
                dp.deliveredDate().equals(VALID_DELIVERED_DATE)));
    }

    @Test
    void STAFF_도착_확인_성공() {
        assertThat(service.deliver(command(UserRole.BRANCH_STAFF)).getStatus())
                .isEqualTo(SalesOrderStatus.DELIVERED);
    }

    @Test
    void 성공시_라인_quantity_유지됨() {
        assertThat(service.deliver(command(UserRole.BRANCH_MANAGER)).getLines())
                .extracting(SalesOrderLine::getQuantity).containsExactly(100, 40);
    }

    @Test
    void 성공시_재고_입고_호출됨() {
        service.deliver(command(UserRole.BRANCH_MANAGER));
        then(inboundStockPort).should().inbound(any(), any());
    }

    @Test
    void 성공시_DELIVERED_저장되고_pending_적재됨() {
        service.deliver(command(UserRole.BRANCH_MANAGER));

        then(saveSalesOrderPort).should().save(argThat(o -> o.getStatus() == SalesOrderStatus.DELIVERED));
        then(pendingStatusChangePort).should().save(argThat(p ->
                p.status() == SalesOrderStatus.DELIVERED && p.payload() instanceof DeliveryPayload));
    }

    @Test
    void HQ_MANAGER_역할_시도시_ForbiddenException() {
        assertThatThrownBy(() -> service.deliver(command(UserRole.HQ_MANAGER)))
                .isInstanceOf(ForbiddenException.class);
        then(loadSalesOrderPort).shouldHaveNoInteractions();
    }

    @Test
    void ADMIN_역할_시도시_ForbiddenException() {
        assertThatThrownBy(() -> service.deliver(command(UserRole.ADMIN)))
                .isInstanceOf(ForbiddenException.class);
        then(loadSalesOrderPort).shouldHaveNoInteractions();
    }

    @Test
    void SO_미존재시_ResourceNotFoundException() {
        given(loadSalesOrderPort.load(SO_CODE))
                .willThrow(new ResourceNotFoundException(SalesErrorCode.SO_NOT_FOUND));

        assertThatThrownBy(() -> service.deliver(command(UserRole.BRANCH_MANAGER)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void 창고_불일치시_ForbiddenException() {
        DeliverSalesOrderCommand command = new DeliverSalesOrderCommand(
                SO_CODE, OTHER_WAREHOUSE, USER_CODE, "정유진", "지점 담당", UserRole.BRANCH_MANAGER, VALID_DELIVERED_DATE);

        assertThatThrownBy(() -> service.deliver(command))
                .isInstanceOf(ForbiddenException.class);
        then(saveSalesOrderPort).shouldHaveNoInteractions();
    }

    @Test
    void deliveredDate가_출고일_이전이면_SalesOrderException() {
        DeliverSalesOrderCommand command = new DeliverSalesOrderCommand(
                SO_CODE, FROM_WAREHOUSE, USER_CODE, "정유진", "지점 담당", UserRole.BRANCH_MANAGER, BEFORE_APPROVED_DATE);

        assertThatThrownBy(() -> service.deliver(command))
                .isInstanceOf(SalesOrderException.class)
                .extracting(e -> ((SalesOrderException) e).getCode())
                .isEqualTo(SalesErrorCode.INVALID_DELIVERED_DATE.getCode());
        then(saveSalesOrderPort).shouldHaveNoInteractions();
    }

    @Test
    void APPROVED_아닌_상태_시도시_InvalidStatusTransitionException() {
        SalesOrder requestedOrder = new SalesOrder(
                SO_CODE, WarehouseRef.of(FROM_WAREHOUSE, "지점"), WarehouseRef.of("WH-HQ-01", "본사"),
                SalesOrderStatus.REQUESTED, SagaStatus.NONE, null,
                new SalesOrderCreation(ACTOR, Instant.now()),
                new SalesOrderRequest(ACTOR, Instant.now()),
                List.of()
        );
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(requestedOrder);

        assertThatThrownBy(() -> service.deliver(command(UserRole.BRANCH_MANAGER)))
                .isInstanceOf(InvalidStatusTransitionException.class);
        then(saveSalesOrderPort).shouldHaveNoInteractions();
    }

    @Test
    void 재고_서비스_실패시_예외_전파되고_저장은_실행됨() {
        willThrow(new ExternalServiceException(
                CommonErrorCode.EXTERNAL_SERVICE_ERROR.getCode(), "재고 서비스 호출 실패", new RuntimeException()))
                .given(inboundStockPort).inbound(any(), any());

        assertThatThrownBy(() -> service.deliver(command(UserRole.BRANCH_MANAGER)))
                .isInstanceOf(ExternalServiceException.class);
        then(saveSalesOrderPort).should().save(any());
    }

    private DeliverSalesOrderCommand command(UserRole role) {
        return new DeliverSalesOrderCommand(SO_CODE, FROM_WAREHOUSE, USER_CODE, "정유진", "지점 담당", role, VALID_DELIVERED_DATE);
    }

    private SalesOrder approvedOrder() {
        List<SalesOrderLine> lines = List.of(
                new SalesOrderLine(1L, SO_CODE, "HMC-EN-00214", "엔진오일", "EA", 100, Priority.NORMAL),
                new SalesOrderLine(2L, SO_CODE, "HMC-BR-01102", "브레이크패드", "EA", 40, Priority.NORMAL)
        );
        // 출고 saga 완료(DONE) 후 입고 시도 — deliver 가드(saga 진행 중 금지) 통과
        return new SalesOrder(
                SO_CODE, WarehouseRef.of(FROM_WAREHOUSE, "지점"), WarehouseRef.of("WH-HQ-01", "본사"),
                SalesOrderStatus.APPROVED, SagaStatus.DONE, null,
                new SalesOrderCreation(ACTOR, Instant.now()),
                new SalesOrderRequest(ACTOR, Instant.now()),
                lines
        );
    }
}
