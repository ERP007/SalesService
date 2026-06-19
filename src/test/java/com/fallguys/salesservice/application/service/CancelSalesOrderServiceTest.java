package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.command.CancelSalesOrderCommand;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.SaveSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.AppendSalesOrderStatusHistoryPort;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.InvalidStatusTransitionException;
import com.fallguys.salesservice.domain.exception.ResourceNotFoundException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.model.*;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderCreation;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderRequest;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.salesorderhistory.CancellationPayload;
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
class CancelSalesOrderServiceTest {

    @Mock LoadSalesOrderPort loadSalesOrderPort;
    @Mock SaveSalesOrderPort saveSalesOrderPort;
    @Mock AppendSalesOrderStatusHistoryPort appendHistoryPort;

    @InjectMocks
    CancelSalesOrderService service;

    private static final String SO_CODE = "SO-2026-06-0001";
    private static final String USER_CODE = "branch001";
    private static final String OTHER_USER_CODE = "branch002";
    private static final String FROM_WAREHOUSE = "WH-BRANCH-01";
    private static final String OTHER_WAREHOUSE = "WH-BRANCH-99";
    private static final String REASON = "지점 재고 확보로 발주 불요";

    @BeforeEach
    void setUp() {
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(requestedOrder(USER_CODE));
        given(saveSalesOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void MANAGER_취소_성공() {
        CancelSalesOrderCommand command = command(USER_CODE, UserRole.BRANCH_MANAGER);

        SalesOrder result = service.cancel(command);

        assertThat(result.getStatus()).isEqualTo(SalesOrderStatus.CANCELED);
        assertThat(result.getCancellation()).isNotNull();
        assertThat(result.getCancellation().canceledBy()).isEqualTo(USER_CODE);
        assertThat(result.getCancellation().cancelReason()).isEqualTo(REASON);
        assertThat(result.getCancellation().canceledAt()).isNotNull();
    }

    @Test
    void STAFF_본인_발주_취소_성공() {
        CancelSalesOrderCommand command = command(USER_CODE, UserRole.BRANCH_STAFF);

        SalesOrder result = service.cancel(command);

        assertThat(result.getStatus()).isEqualTo(SalesOrderStatus.CANCELED);
    }

    @Test
    void MANAGER_타인_발주_취소_성공() {
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(requestedOrder(OTHER_USER_CODE));
        CancelSalesOrderCommand command = command(USER_CODE, UserRole.BRANCH_MANAGER);

        SalesOrder result = service.cancel(command);

        assertThat(result.getStatus()).isEqualTo(SalesOrderStatus.CANCELED);
    }

    @Test
    void HQ_역할_취소_시도시_ForbiddenException() {
        CancelSalesOrderCommand command = command(USER_CODE, UserRole.HQ_MANAGER);

        assertThatThrownBy(() -> service.cancel(command))
                .isInstanceOf(ForbiddenException.class);

        then(loadSalesOrderPort).shouldHaveNoInteractions();
    }

    @Test
    void ADMIN_역할_취소_시도시_ForbiddenException() {
        CancelSalesOrderCommand command = command(USER_CODE, UserRole.ADMIN);

        assertThatThrownBy(() -> service.cancel(command))
                .isInstanceOf(ForbiddenException.class);

        then(loadSalesOrderPort).shouldHaveNoInteractions();
    }

    @Test
    void SO_미존재시_ResourceNotFoundException() {
        given(loadSalesOrderPort.load(SO_CODE))
                .willThrow(new ResourceNotFoundException(SalesErrorCode.SO_NOT_FOUND));

        assertThatThrownBy(() -> service.cancel(command(USER_CODE, UserRole.BRANCH_STAFF)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void 소속_창고_불일치시_ForbiddenException() {
        CancelSalesOrderCommand badCommand = new CancelSalesOrderCommand(SO_CODE, USER_CODE, UserRole.BRANCH_MANAGER, OTHER_WAREHOUSE, REASON);

        assertThatThrownBy(() -> service.cancel(badCommand))
                .isInstanceOf(ForbiddenException.class);

        then(saveSalesOrderPort).shouldHaveNoInteractions();
    }

    @Test
    void STAFF_타인_발주_취소_시도시_ForbiddenException() {
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(requestedOrder(OTHER_USER_CODE));

        assertThatThrownBy(() -> service.cancel(command(USER_CODE, UserRole.BRANCH_STAFF)))
                .isInstanceOf(ForbiddenException.class);

        then(saveSalesOrderPort).shouldHaveNoInteractions();
    }

    @Test
    void REQUESTED_아닌_상태_취소_시도시_InvalidStatusTransitionException() {
        SalesOrder draftOrder = new SalesOrder(
                SO_CODE, FROM_WAREHOUSE, "WH-HQ-01",
                SalesOrderStatus.DRAFT, LocalDate.now().plusDays(3), null,
                new SalesOrderCreation(USER_CODE, Instant.now()),
                null, null, null, null, null, List.of()
        );
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(draftOrder);

        assertThatThrownBy(() -> service.cancel(command(USER_CODE, UserRole.BRANCH_MANAGER)))
                .isInstanceOf(InvalidStatusTransitionException.class);

        then(saveSalesOrderPort).shouldHaveNoInteractions();
    }

    @Test
    void STAFF_REQUESTED_아닌_상태_취소_시도시_InvalidStatusTransitionException() {
        SalesOrder draftOrder = new SalesOrder(
                SO_CODE, FROM_WAREHOUSE, "WH-HQ-01",
                SalesOrderStatus.DRAFT, LocalDate.now().plusDays(3), null,
                new SalesOrderCreation(USER_CODE, Instant.now()),
                null, null, null, null, null, List.of()
        );
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(draftOrder);

        assertThatThrownBy(() -> service.cancel(command(USER_CODE, UserRole.BRANCH_STAFF)))
                .isInstanceOf(InvalidStatusTransitionException.class);

        then(saveSalesOrderPort).shouldHaveNoInteractions();
    }

    @Test
    void 취소_성공시_CANCELED_상태로_저장됨() {
        CancelSalesOrderCommand command = command(USER_CODE, UserRole.BRANCH_MANAGER);

        service.cancel(command);

        then(saveSalesOrderPort).should().save(argThat(o ->
                o.getStatus() == SalesOrderStatus.CANCELED &&
                o.getCancellation() != null
        ));
        then(appendHistoryPort).should().append(argThat(h ->
                h.status() == SalesOrderStatus.CANCELED &&
                h.payload() instanceof CancellationPayload p &&
                p.cancelReason().equals(REASON)
        ));
    }

    private CancelSalesOrderCommand command(String userCode, UserRole role) {
        return new CancelSalesOrderCommand(SO_CODE, userCode, role, FROM_WAREHOUSE, REASON);
    }

    private SalesOrder requestedOrder(String requestedBy) {
        return new SalesOrder(
                SO_CODE, FROM_WAREHOUSE, "WH-HQ-01",
                SalesOrderStatus.REQUESTED, LocalDate.now().plusDays(3), null,
                new SalesOrderCreation(requestedBy, Instant.now()),
                new SalesOrderRequest(requestedBy, Instant.now()),
                null, null, null, null, List.of()
        );
    }
}
