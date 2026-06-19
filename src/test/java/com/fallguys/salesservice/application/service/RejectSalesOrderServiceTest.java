package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.command.RejectSalesOrderCommand;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.SaveSalesOrderPort;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.InvalidStatusTransitionException;
import com.fallguys.salesservice.domain.exception.ResourceNotFoundException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.exception.SalesOrderException;
import com.fallguys.salesservice.domain.model.*;
import com.fallguys.salesservice.domain.model.salesorder.*;
import com.fallguys.salesservice.domain.model.salesorderhistory.CarrierType;
import com.fallguys.salesservice.domain.model.salesorderhistory.RejectReasonCategory;
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
class RejectSalesOrderServiceTest {

    @Mock LoadSalesOrderPort loadSalesOrderPort;
    @Mock SaveSalesOrderPort saveSalesOrderPort;

    @InjectMocks
    RejectSalesOrderService service;

    private static final String SO_CODE = "SO-2026-06-0001";
    private static final String HQ_USER_CODE = "HQ-001";
    private static final String FROM_WAREHOUSE = "WH-BRANCH-01";

    @BeforeEach
    void setUp() {
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(requestedOrder());
        given(saveSalesOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void ADMIN_반려_성공() {
        SalesOrder result = service.reject(command(UserRole.ADMIN, RejectReasonCategory.OUT_OF_STOCK, null));

        assertThat(result.getStatus()).isEqualTo(SalesOrderStatus.REJECTED);
        assertThat(result.getRejection()).isNotNull();
        assertThat(result.getRejection().rejectedBy()).isEqualTo(HQ_USER_CODE);
        assertThat(result.getRejection().rejectReasonCategory()).isEqualTo(RejectReasonCategory.OUT_OF_STOCK);
        assertThat(result.getRejection().rejectedAt()).isNotNull();
    }

    @Test
    void HQ_MANAGER_반려_성공() {
        SalesOrder result = service.reject(command(UserRole.HQ_MANAGER, RejectReasonCategory.POLICY, null));

        assertThat(result.getStatus()).isEqualTo(SalesOrderStatus.REJECTED);
    }

    @Test
    void HQ_STAFF_반려_시도시_ForbiddenException() {
        assertThatThrownBy(() -> service.reject(command(UserRole.HQ_STAFF, RejectReasonCategory.OUT_OF_STOCK, null)))
                .isInstanceOf(ForbiddenException.class);

        then(loadSalesOrderPort).shouldHaveNoInteractions();
    }

    @Test
    void BRANCH_역할_반려_시도시_ForbiddenException() {
        assertThatThrownBy(() -> service.reject(command(UserRole.BRANCH_MANAGER, RejectReasonCategory.OUT_OF_STOCK, null)))
                .isInstanceOf(ForbiddenException.class);

        then(loadSalesOrderPort).shouldHaveNoInteractions();
    }

    @Test
    void OTHER_memo_null이면_SalesOrderException() {
        assertThatThrownBy(() -> service.reject(command(UserRole.ADMIN, RejectReasonCategory.OTHER, null)))
                .isInstanceOf(SalesOrderException.class)
                .satisfies(ex -> assertThat(((SalesOrderException) ex).getCode())
                        .isEqualTo(SalesErrorCode.REJECT_MEMO_REQUIRED.getCode()));

        then(loadSalesOrderPort).shouldHaveNoInteractions();
    }

    @Test
    void OTHER_memo_공백이면_SalesOrderException() {
        assertThatThrownBy(() -> service.reject(command(UserRole.ADMIN, RejectReasonCategory.OTHER, "   ")))
                .isInstanceOf(SalesOrderException.class);

        then(loadSalesOrderPort).shouldHaveNoInteractions();
    }

    @Test
    void OTHER_memo_있으면_반려_성공() {
        SalesOrder result = service.reject(command(UserRole.ADMIN, RejectReasonCategory.OTHER, "기타 사유 상세 설명"));

        assertThat(result.getStatus()).isEqualTo(SalesOrderStatus.REJECTED);
        assertThat(result.getRejection().rejectReasonMemo()).isEqualTo("기타 사유 상세 설명");
    }

    @Test
    void SO_미존재시_ResourceNotFoundException() {
        given(loadSalesOrderPort.load(SO_CODE))
                .willThrow(new ResourceNotFoundException(SalesErrorCode.SO_NOT_FOUND));

        assertThatThrownBy(() -> service.reject(command(UserRole.ADMIN, RejectReasonCategory.OUT_OF_STOCK, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void REQUESTED_아닌_상태_반려_시도시_InvalidStatusTransitionException() {
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(approvedOrder());

        assertThatThrownBy(() -> service.reject(command(UserRole.ADMIN, RejectReasonCategory.OUT_OF_STOCK, null)))
                .isInstanceOf(InvalidStatusTransitionException.class);

        then(saveSalesOrderPort).shouldHaveNoInteractions();
    }

    @Test
    void 반려_성공시_REJECTED_상태로_저장됨() {
        service.reject(command(UserRole.HQ_MANAGER, RejectReasonCategory.DUPLICATE, "중복 발주 확인"));

        then(saveSalesOrderPort).should().save(argThat(o ->
                o.getStatus() == SalesOrderStatus.REJECTED &&
                o.getRejection() != null &&
                o.getRejection().rejectReasonCategory() == RejectReasonCategory.DUPLICATE
        ));
    }

    private RejectSalesOrderCommand command(UserRole role, RejectReasonCategory category, String memo) {
        return new RejectSalesOrderCommand(SO_CODE, HQ_USER_CODE, role, category, memo);
    }

    private SalesOrder requestedOrder() {
        return new SalesOrder(
                SO_CODE, FROM_WAREHOUSE, "WH-HQ-01",
                SalesOrderStatus.REQUESTED, LocalDate.now().plusDays(3), null,
                new SalesOrderCreation(HQ_USER_CODE, Instant.now()),
                new SalesOrderRequest(HQ_USER_CODE, Instant.now()),
                null, null, null, null, List.of()
        );
    }

    private SalesOrder approvedOrder() {
        return new SalesOrder(
                SO_CODE, FROM_WAREHOUSE, "WH-HQ-01",
                SalesOrderStatus.APPROVED, LocalDate.now().plusDays(3), null,
                new SalesOrderCreation(HQ_USER_CODE, Instant.now()),
                new SalesOrderRequest(HQ_USER_CODE, Instant.now()),
                new SalesOrderApproval(HQ_USER_CODE, Instant.now(), LocalDate.now(), CarrierType.VEHICLE, "INV-001"),
                null, null, null, List.of()
        );
    }
}
