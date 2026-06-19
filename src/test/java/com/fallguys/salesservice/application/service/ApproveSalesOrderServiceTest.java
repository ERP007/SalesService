package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.command.ApproveSalesOrderCommand;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.OutboundStockPort;
import com.fallguys.salesservice.application.port.outbound.port.SaveSalesOrderPort;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.InvalidStatusTransitionException;
import com.fallguys.salesservice.domain.exception.ResourceNotFoundException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.exception.SalesOrderException;
import com.fallguys.salesservice.domain.model.*;
import com.fallguys.salesservice.domain.model.salesorder.*;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApproveSalesOrderServiceTest {

    @Mock LoadSalesOrderPort loadSalesOrderPort;
    @Mock SaveSalesOrderPort saveSalesOrderPort;
    @Mock OutboundStockPort outboundStockPort;

    @InjectMocks
    ApproveSalesOrderService service;

    private static final String SO_CODE = "SO-2026-06-0001";
    private static final String APPROVED_BY = "hq001";
    private static final String INVOICE_NUMBER = "INV-2026-0001";
    private static final LocalDate TODAY = LocalDate.of(2026, 6, 8);
    private static final Instant REQUESTED_AT = TODAY.minusDays(1).atStartOfDay()
            .atZone(java.time.ZoneId.of("Asia/Seoul")).toInstant();

    @BeforeEach
    void setUp() {
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(requestedOrder());
        given(loadSalesOrderPort.existsByInvoiceNumber(any())).willReturn(false);
        given(saveSalesOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));
        willDoNothing().given(outboundStockPort).outbound(any());
    }

    // ── 성공 케이스 ──────────────────────────────────────────────────────────

    @Test
    void ADMIN_승인_성공() {
        ApproveSalesOrderCommand command = command(UserRole.ADMIN, TODAY, INVOICE_NUMBER);

        SalesOrder result = service.approve(command);

        assertThat(result.getStatus()).isEqualTo(SalesOrderStatus.APPROVED);
        assertThat(result.getApproval()).isNotNull();
        assertThat(result.getApproval().approvedBy()).isEqualTo(APPROVED_BY);
        assertThat(result.getApproval().approvedDate()).isEqualTo(TODAY);
        assertThat(result.getApproval().carrierType()).isEqualTo(CarrierType.VEHICLE);
        assertThat(result.getApproval().invoiceNumber()).isEqualTo(INVOICE_NUMBER);
    }

    @Test
    void HQ_MANAGER_승인_성공() {
        ApproveSalesOrderCommand command = command(UserRole.HQ_MANAGER, TODAY, INVOICE_NUMBER);

        SalesOrder result = service.approve(command);

        assertThat(result.getStatus()).isEqualTo(SalesOrderStatus.APPROVED);
    }

    @Test
    void HQ_STAFF_승인_성공() {
        ApproveSalesOrderCommand command = command(UserRole.HQ_STAFF, TODAY, INVOICE_NUMBER);

        SalesOrder result = service.approve(command);

        assertThat(result.getStatus()).isEqualTo(SalesOrderStatus.APPROVED);
    }

    @Test
    void invoiceNumber_null_허용_중복체크_미실행() {
        ApproveSalesOrderCommand command = command(UserRole.ADMIN, TODAY, null);

        SalesOrder result = service.approve(command);

        assertThat(result.getStatus()).isEqualTo(SalesOrderStatus.APPROVED);
        then(loadSalesOrderPort).should(never()).existsByInvoiceNumber(any());
    }

    @Test
    void 승인일_요청일과_동일해도_허용() {
        LocalDate requestedDate = REQUESTED_AT.atZone(java.time.ZoneId.of("Asia/Seoul")).toLocalDate();
        ApproveSalesOrderCommand command = command(UserRole.ADMIN, requestedDate, INVOICE_NUMBER);

        SalesOrder result = service.approve(command);

        assertThat(result.getStatus()).isEqualTo(SalesOrderStatus.APPROVED);
    }

    @Test
    void 라인_approvedQuantity_requestedQuantity로_확정됨() {
        ApproveSalesOrderCommand command = command(UserRole.ADMIN, TODAY, INVOICE_NUMBER);

        SalesOrder result = service.approve(command);

        assertThat(result.getLines()).allSatisfy(line ->
                assertThat(line.getApprovedQuantity()).isEqualTo(line.getRequestedQuantity())
        );
    }

    @Test
    void 승인_성공시_재고_출고_호출됨() {
        ApproveSalesOrderCommand command = command(UserRole.ADMIN, TODAY, INVOICE_NUMBER);

        service.approve(command);

        then(outboundStockPort).should().outbound(any(SalesOrder.class));
    }

    @Test
    void 승인_성공시_APPROVED_상태로_저장됨() {
        ApproveSalesOrderCommand command = command(UserRole.ADMIN, TODAY, INVOICE_NUMBER);

        service.approve(command);

        then(saveSalesOrderPort).should().save(argThat(o ->
                o.getStatus() == SalesOrderStatus.APPROVED &&
                o.getApproval() != null
        ));
    }

    // ── 역할 검증 ─────────────────────────────────────────────────────────────

    @Test
    void BRANCH_MANAGER_역할_승인_시도시_ForbiddenException() {
        ApproveSalesOrderCommand command = command(UserRole.BRANCH_MANAGER, TODAY, INVOICE_NUMBER);

        assertThatThrownBy(() -> service.approve(command))
                .isInstanceOf(ForbiddenException.class);

        then(loadSalesOrderPort).shouldHaveNoInteractions();
    }

    @Test
    void BRANCH_STAFF_역할_승인_시도시_ForbiddenException() {
        ApproveSalesOrderCommand command = command(UserRole.BRANCH_STAFF, TODAY, INVOICE_NUMBER);

        assertThatThrownBy(() -> service.approve(command))
                .isInstanceOf(ForbiddenException.class);

        then(loadSalesOrderPort).shouldHaveNoInteractions();
    }

    // ── 송장 번호 중복 ────────────────────────────────────────────────────────

    @Test
    void 송장번호_중복시_SalesOrderException_DUPLICATE_INVOICE_NUMBER() {
        given(loadSalesOrderPort.existsByInvoiceNumber(INVOICE_NUMBER)).willReturn(true);
        ApproveSalesOrderCommand command = command(UserRole.ADMIN, TODAY, INVOICE_NUMBER);

        assertThatThrownBy(() -> service.approve(command))
                .isInstanceOf(SalesOrderException.class)
                .hasMessageContaining(SalesErrorCode.DUPLICATE_INVOICE_NUMBER.getDefaultMessage());

        then(loadSalesOrderPort).should(never()).load(any());
    }

    // ── SO 미존재 ─────────────────────────────────────────────────────────────

    @Test
    void SO_미존재시_ResourceNotFoundException() {
        given(loadSalesOrderPort.load(SO_CODE))
                .willThrow(new ResourceNotFoundException(SalesErrorCode.SO_NOT_FOUND));
        ApproveSalesOrderCommand command = command(UserRole.ADMIN, TODAY, INVOICE_NUMBER);

        assertThatThrownBy(() -> service.approve(command))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── 승인일 검증 ───────────────────────────────────────────────────────────

    @Test
    void 승인일이_요청일보다_이전이면_SalesOrderException_INVALID_APPROVED_DATE() {
        LocalDate requestedDate = REQUESTED_AT.atZone(java.time.ZoneId.of("Asia/Seoul")).toLocalDate();
        LocalDate tooEarly = requestedDate.minusDays(1);
        ApproveSalesOrderCommand command = command(UserRole.ADMIN, tooEarly, INVOICE_NUMBER);

        assertThatThrownBy(() -> service.approve(command))
                .isInstanceOf(SalesOrderException.class)
                .hasMessageContaining(requestedDate.toString());

        then(saveSalesOrderPort).shouldHaveNoInteractions();
    }

    // ── 상태 전환 검증 ────────────────────────────────────────────────────────

    @Test
    void REQUESTED_아닌_상태_승인_시도시_InvalidStatusTransitionException() {
        SalesOrder draftOrder = new SalesOrder(
                SO_CODE, "WH-BRANCH-01", "WH-HQ-01",
                SalesOrderStatus.DRAFT, TODAY.plusDays(3), null,
                new SalesOrderCreation("branch001", Instant.now()),
                null, null, null, null, null, List.of()
        );
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(draftOrder);
        ApproveSalesOrderCommand command = command(UserRole.ADMIN, TODAY, INVOICE_NUMBER);

        assertThatThrownBy(() -> service.approve(command))
                .isInstanceOf(InvalidStatusTransitionException.class);

        then(saveSalesOrderPort).shouldHaveNoInteractions();
    }

    @Test
    void APPROVED_상태_재승인_시도시_InvalidStatusTransitionException() {
        SalesOrder approvedOrder = new SalesOrder(
                SO_CODE, "WH-BRANCH-01", "WH-HQ-01",
                SalesOrderStatus.APPROVED, TODAY.plusDays(3), null,
                new SalesOrderCreation("branch001", Instant.now()),
                new SalesOrderRequest("branch001", REQUESTED_AT),
                new SalesOrderApproval(APPROVED_BY, Instant.now(), TODAY, CarrierType.VEHICLE, INVOICE_NUMBER),
                null, null, null, List.of()
        );
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(approvedOrder);
        ApproveSalesOrderCommand command = command(UserRole.ADMIN, TODAY, "INV-OTHER");

        assertThatThrownBy(() -> service.approve(command))
                .isInstanceOf(InvalidStatusTransitionException.class);

        then(saveSalesOrderPort).shouldHaveNoInteractions();
    }

    // ── 재고 출고 실패 ─────────────────────────────────────────────────────────

    @Test
    void 재고_출고_실패시_예외_전파되고_저장은_실행됨() {
        willThrow(new SalesOrderException(SalesErrorCode.INVENTORY_OUTBOUND_FAILED))
                .given(outboundStockPort).outbound(any());
        ApproveSalesOrderCommand command = command(UserRole.ADMIN, TODAY, INVOICE_NUMBER);

        assertThatThrownBy(() -> service.approve(command))
                .isInstanceOf(SalesOrderException.class);

        then(saveSalesOrderPort).should().save(any());
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    private ApproveSalesOrderCommand command(UserRole role, LocalDate approvedDate, String invoiceNumber) {
        return new ApproveSalesOrderCommand(SO_CODE, APPROVED_BY, role, approvedDate, CarrierType.VEHICLE, invoiceNumber);
    }

    private SalesOrder requestedOrder() {
        SalesOrderLine line = new SalesOrderLine(1L, SO_CODE, "ITEM-001", "브레이크 패드", "EA", 10, null, null, Priority.NORMAL);
        return new SalesOrder(
                SO_CODE, "WH-BRANCH-01", "WH-HQ-01",
                SalesOrderStatus.REQUESTED, TODAY.plusDays(3), null,
                new SalesOrderCreation("branch001", REQUESTED_AT.minusSeconds(60)),
                new SalesOrderRequest("branch001", REQUESTED_AT),
                null, null, null, null, List.of(line)
        );
    }
}
