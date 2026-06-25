package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.command.ApproveSalesOrderCommand;
import com.fallguys.salesservice.application.port.outbound.port.AppendSalesOrderStatusHistoryPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.OutboundStockPort;
import com.fallguys.salesservice.application.port.outbound.port.PendingStatusChangePort;
import com.fallguys.salesservice.application.port.outbound.port.SaveSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.SyncOutboundStockPort;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApproveSalesOrderServiceTest {

    @Mock LoadSalesOrderPort loadSalesOrderPort;
    @Mock SaveSalesOrderPort saveSalesOrderPort;
    @Mock PendingStatusChangePort pendingStatusChangePort;
    @Mock OutboundStockPort outboundStockPort;
    @Mock SyncOutboundStockPort syncOutboundStockPort;
    @Mock AppendSalesOrderStatusHistoryPort appendHistoryPort;

    @Mock UserActivityRecorder userActivityRecorder;

    @InjectMocks
    ApproveSalesOrderService service;

    private static final String SO_CODE = "SO-2026-06-0001";
    private static final String APPROVED_BY = "hq001";
    private static final String INVOICE_NUMBER = "INV-2026-0001";
    private static final LocalDate TODAY = LocalDate.of(2026, 6, 8);
    private static final Instant REQUESTED_AT = TODAY.minusDays(1).atStartOfDay()
            .atZone(ZoneId.of("Asia/Seoul")).toInstant();

    private static final ActorRef BRANCH_ACTOR = ActorRef.of("branch001", "정유진", "지점 담당");

    @BeforeEach
    void setUp() {
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(requestedOrder());
        given(saveSalesOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));
        willDoNothing().given(outboundStockPort).outbound(any(), any());
    }

    @Test
    void ADMIN_승인_성공() {
        SalesOrder result = service.approve(command(UserRole.ADMIN, TODAY, INVOICE_NUMBER));

        assertThat(result.getStatus()).isEqualTo(SalesOrderStatus.APPROVED);
        then(pendingStatusChangePort).should().save(argThat(p ->
                p.status() == SalesOrderStatus.APPROVED &&
                p.actor().code().equals(APPROVED_BY) &&
                p.payload() instanceof ApprovalPayload ap &&
                ap.approvedDate().equals(TODAY) &&
                ap.carrierType() == CarrierType.VEHICLE &&
                ap.invoiceNumber().equals(INVOICE_NUMBER)));
    }

    @Test
    void HQ_MANAGER_승인_성공() {
        assertThat(service.approve(command(UserRole.HQ_MANAGER, TODAY, INVOICE_NUMBER)).getStatus())
                .isEqualTo(SalesOrderStatus.APPROVED);
    }

    @Test
    void HQ_STAFF_승인_성공() {
        assertThat(service.approve(command(UserRole.HQ_STAFF, TODAY, INVOICE_NUMBER)).getStatus())
                .isEqualTo(SalesOrderStatus.APPROVED);
    }

    @Test
    void invoiceNumber_null_허용() {
        assertThat(service.approve(command(UserRole.ADMIN, TODAY, null)).getStatus())
                .isEqualTo(SalesOrderStatus.APPROVED);
    }

    @Test
    void 승인일_요청일과_동일해도_허용() {
        LocalDate requestedDate = REQUESTED_AT.atZone(ZoneId.of("Asia/Seoul")).toLocalDate();
        assertThat(service.approve(command(UserRole.ADMIN, requestedDate, INVOICE_NUMBER)).getStatus())
                .isEqualTo(SalesOrderStatus.APPROVED);
    }

    @Test
    void 승인_성공시_재고_출고_호출됨() {
        service.approve(command(UserRole.ADMIN, TODAY, INVOICE_NUMBER));
        then(outboundStockPort).should().outbound(any(SalesOrder.class), any());
    }

    @Test
    void 승인_성공시_APPROVED_상태로_저장되고_pending에_적재됨() {
        service.approve(command(UserRole.ADMIN, TODAY, INVOICE_NUMBER));

        then(saveSalesOrderPort).should().save(argThat(o -> o.getStatus() == SalesOrderStatus.APPROVED));
        then(pendingStatusChangePort).should().save(argThat(p ->
                p.status() == SalesOrderStatus.APPROVED &&
                p.payload() instanceof ApprovalPayload ap &&
                ap.invoiceNumber().equals(INVOICE_NUMBER)));
    }

    @Test
    void BRANCH_MANAGER_역할_승인_시도시_ForbiddenException() {
        assertThatThrownBy(() -> service.approve(command(UserRole.BRANCH_MANAGER, TODAY, INVOICE_NUMBER)))
                .isInstanceOf(ForbiddenException.class);
        then(loadSalesOrderPort).shouldHaveNoInteractions();
    }

    @Test
    void BRANCH_STAFF_역할_승인_시도시_ForbiddenException() {
        assertThatThrownBy(() -> service.approve(command(UserRole.BRANCH_STAFF, TODAY, INVOICE_NUMBER)))
                .isInstanceOf(ForbiddenException.class);
        then(loadSalesOrderPort).shouldHaveNoInteractions();
    }

    @Test
    void SO_미존재시_ResourceNotFoundException() {
        given(loadSalesOrderPort.load(SO_CODE))
                .willThrow(new ResourceNotFoundException(SalesErrorCode.SO_NOT_FOUND));

        assertThatThrownBy(() -> service.approve(command(UserRole.ADMIN, TODAY, INVOICE_NUMBER)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void 승인일이_요청일보다_이전이면_SalesOrderException() {
        LocalDate requestedDate = REQUESTED_AT.atZone(ZoneId.of("Asia/Seoul")).toLocalDate();
        LocalDate tooEarly = requestedDate.minusDays(1);

        assertThatThrownBy(() -> service.approve(command(UserRole.ADMIN, tooEarly, INVOICE_NUMBER)))
                .isInstanceOf(SalesOrderException.class)
                .hasMessageContaining(requestedDate.toString());
        then(saveSalesOrderPort).shouldHaveNoInteractions();
    }

    @Test
    void REQUESTED_아닌_상태_승인_시도시_InvalidStatusTransitionException() {
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(order(SalesOrderStatus.DRAFT, null));

        assertThatThrownBy(() -> service.approve(command(UserRole.ADMIN, TODAY, INVOICE_NUMBER)))
                .isInstanceOf(InvalidStatusTransitionException.class);
        then(saveSalesOrderPort).shouldHaveNoInteractions();
    }

    @Test
    void APPROVED_상태_재승인_시도시_InvalidStatusTransitionException() {
        given(loadSalesOrderPort.load(SO_CODE))
                .willReturn(order(SalesOrderStatus.APPROVED, new SalesOrderRequest(BRANCH_ACTOR, REQUESTED_AT)));

        assertThatThrownBy(() -> service.approve(command(UserRole.ADMIN, TODAY, "INV-OTHER")))
                .isInstanceOf(InvalidStatusTransitionException.class);
        then(saveSalesOrderPort).shouldHaveNoInteractions();
    }

    @Test
    void 재고_출고_실패시_예외_전파되고_저장은_실행됨() {
        willThrow(new SalesOrderException(SalesErrorCode.INVENTORY_OUTBOUND_FAILED))
                .given(outboundStockPort).outbound(any(), any());

        assertThatThrownBy(() -> service.approve(command(UserRole.ADMIN, TODAY, INVOICE_NUMBER)))
                .isInstanceOf(SalesOrderException.class);
        then(saveSalesOrderPort).should().save(any());
    }

    @Test
    void 발주_라인이_없으면_SalesOrderException() {
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(new SalesOrder(
                SO_CODE, WarehouseRef.of("WH-BRANCH-01", "지점"), WarehouseRef.of("WH-HQ-01", "본사"),
                SalesOrderStatus.REQUESTED, SagaStatus.NONE, null,
                new SalesOrderCreation(BRANCH_ACTOR, REQUESTED_AT.minusSeconds(60)),
                new SalesOrderRequest(BRANCH_ACTOR, REQUESTED_AT),
                List.of(), null));

        assertThatThrownBy(() -> service.approve(command(UserRole.ADMIN, TODAY, INVOICE_NUMBER)))
                .isInstanceOf(SalesOrderException.class)
                .extracting("code")
                .isEqualTo(SalesErrorCode.EMPTY_ORDER_LINES.getCode());
        then(saveSalesOrderPort).shouldHaveNoInteractions();
        then(outboundStockPort).shouldHaveNoInteractions();
    }

    @Test
    void sync모드_승인_성공시_동기_출고호출_saga_DONE_이력_즉시기록() {
        ReflectionTestUtils.setField(service, "stockSyncMode", true);

        SalesOrder result = service.approve(command(UserRole.ADMIN, TODAY, INVOICE_NUMBER));

        assertThat(result.getStatus()).isEqualTo(SalesOrderStatus.APPROVED);
        assertThat(result.getSagaStatus()).isEqualTo(SagaStatus.DONE);
        then(syncOutboundStockPort).should().outbound(any(SalesOrder.class));
        then(appendHistoryPort).should().append(any());
        // sync 경로는 staging/outbox를 쓰지 않는다.
        then(pendingStatusChangePort).shouldHaveNoInteractions();
        then(outboundStockPort).shouldHaveNoInteractions();
    }

    @Test
    void sync모드_동기_출고_실패시_예외전파_이력없음() {
        ReflectionTestUtils.setField(service, "stockSyncMode", true);
        willThrow(new SalesOrderException(SalesErrorCode.INVENTORY_OUTBOUND_FAILED))
                .given(syncOutboundStockPort).outbound(any(SalesOrder.class));

        assertThatThrownBy(() -> service.approve(command(UserRole.ADMIN, TODAY, INVOICE_NUMBER)))
                .isInstanceOf(SalesOrderException.class);
        then(appendHistoryPort).shouldHaveNoInteractions();
    }

    private ApproveSalesOrderCommand command(UserRole role, LocalDate approvedDate, String invoiceNumber) {
        return new ApproveSalesOrderCommand(
                SO_CODE, APPROVED_BY, "강지석", "본사 매니저", role, approvedDate, CarrierType.VEHICLE, invoiceNumber);
    }

    private SalesOrder requestedOrder() {
        SalesOrderLine line = new SalesOrderLine(1L, SO_CODE, "ITEM-001", "브레이크 패드", "EA", 10, Priority.NORMAL);
        return new SalesOrder(
                SO_CODE, WarehouseRef.of("WH-BRANCH-01", "지점"), WarehouseRef.of("WH-HQ-01", "본사"),
                SalesOrderStatus.REQUESTED, SagaStatus.NONE, null,
                new SalesOrderCreation(BRANCH_ACTOR, REQUESTED_AT.minusSeconds(60)),
                new SalesOrderRequest(BRANCH_ACTOR, REQUESTED_AT),
                List.of(line),
                null
        );
    }

    private SalesOrder order(SalesOrderStatus status, SalesOrderRequest request) {
        return new SalesOrder(
                SO_CODE, WarehouseRef.of("WH-BRANCH-01", "지점"), WarehouseRef.of("WH-HQ-01", "본사"),
                status, SagaStatus.NONE, null,
                new SalesOrderCreation(BRANCH_ACTOR, Instant.now()),
                request,
                List.of(new SalesOrderLine(1L, SO_CODE, "ITEM-001", "브레이크 패드", "EA", 10, Priority.NORMAL)),
                null
        );
    }
}
