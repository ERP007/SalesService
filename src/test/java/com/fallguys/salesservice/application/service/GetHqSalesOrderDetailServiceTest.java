package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.GetHqSalesOrderDetailQuery;
import com.fallguys.salesservice.application.port.inbound.HqSalesOrderDetail;
import com.fallguys.salesservice.application.port.outbound.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.LoadUserInfoPort;
import com.fallguys.salesservice.application.port.outbound.LoadWarehousePort;
import com.fallguys.salesservice.application.port.outbound.UserInfo;
import com.fallguys.salesservice.application.port.outbound.WarehouseInfo;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.ResourceNotFoundException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.model.*;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GetHqSalesOrderDetailServiceTest {

    @Mock LoadSalesOrderPort loadSalesOrderPort;
    @Mock LoadWarehousePort loadWarehousePort;
    @Mock LoadUserInfoPort loadUserInfoPort;

    @InjectMocks
    GetHqSalesOrderDetailService service;

    private static final String SO_CODE = "SO-2026-06-0001";
    private static final String REQUESTER_CODE = "BR-001";
    private static final String APPROVER_CODE = "HQ-001";
    private static final String FROM_WAREHOUSE = "WH-BRANCH-01";
    private static final String FROM_WAREHOUSE_NAME = "강남 1지점";
    private static final String TO_WAREHOUSE = "WH-HQ-01";
    private static final String TO_WAREHOUSE_NAME = "본사 중앙창고";
    private static final Instant FIXED_INSTANT = Instant.parse("2026-06-08T09:00:00Z");
    private static final LocalDate FIXED_DATE = LocalDate.of(2026, 6, 8);

    @BeforeEach
    void setUp() {
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(requestedOrder());
        given(loadWarehousePort.load(FROM_WAREHOUSE)).willReturn(new WarehouseInfo(FROM_WAREHOUSE, FROM_WAREHOUSE_NAME));
        given(loadWarehousePort.load(TO_WAREHOUSE)).willReturn(new WarehouseInfo(TO_WAREHOUSE, TO_WAREHOUSE_NAME));
        given(loadUserInfoPort.loadByUserCodes(List.of(REQUESTER_CODE)))
                .willReturn(Map.of(REQUESTER_CODE, new UserInfo(REQUESTER_CODE, "정유진", "서비스 매니저")));
    }

    @Test
    void HQ_MANAGER_상세_조회_성공() {
        HqSalesOrderDetail result = service.get(query(UserRole.HQ_MANAGER));

        assertThat(result.salesOrder().getCode()).isEqualTo(SO_CODE);
        assertThat(result.fromWarehouseName()).isEqualTo(FROM_WAREHOUSE_NAME);
        assertThat(result.toWarehouseName()).isEqualTo(TO_WAREHOUSE_NAME);
    }

    @Test
    void HQ_STAFF_상세_조회_성공() {
        HqSalesOrderDetail result = service.get(query(UserRole.HQ_STAFF));

        assertThat(result.salesOrder().getCode()).isEqualTo(SO_CODE);
    }

    @Test
    void ADMIN_상세_조회_성공() {
        HqSalesOrderDetail result = service.get(query(UserRole.ADMIN));

        assertThat(result.salesOrder().getCode()).isEqualTo(SO_CODE);
    }

    @Test
    void BRANCH_MANAGER_조회_시도시_ForbiddenException() {
        assertThatThrownBy(() -> service.get(query(UserRole.BRANCH_MANAGER)))
                .isInstanceOf(ForbiddenException.class);

        then(loadSalesOrderPort).shouldHaveNoInteractions();
        then(loadWarehousePort).shouldHaveNoInteractions();
        then(loadUserInfoPort).shouldHaveNoInteractions();
    }

    @Test
    void BRANCH_STAFF_조회_시도시_ForbiddenException() {
        assertThatThrownBy(() -> service.get(query(UserRole.BRANCH_STAFF)))
                .isInstanceOf(ForbiddenException.class);

        then(loadSalesOrderPort).shouldHaveNoInteractions();
        then(loadWarehousePort).shouldHaveNoInteractions();
        then(loadUserInfoPort).shouldHaveNoInteractions();
    }

    @Test
    void SO_미존재시_ResourceNotFoundException() {
        given(loadSalesOrderPort.load(SO_CODE))
                .willThrow(new ResourceNotFoundException(SalesErrorCode.SO_NOT_FOUND));

        assertThatThrownBy(() -> service.get(query(UserRole.HQ_MANAGER)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void toWarehouseCode_null이면_toWarehouseName_null_반환() {
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(orderWithoutToWarehouse());

        HqSalesOrderDetail result = service.get(query(UserRole.HQ_MANAGER));

        assertThat(result.toWarehouseName()).isNull();
        then(loadWarehousePort).should(never()).load(null);
    }

    @Test
    void requesterInfo_조회_후_반환됨() {
        HqSalesOrderDetail result = service.get(query(UserRole.HQ_MANAGER));

        assertThat(result.requesterInfo()).isNotNull();
        assertThat(result.requesterInfo().name()).isEqualTo("정유진");
        assertThat(result.requesterInfo().position()).isEqualTo("서비스 매니저");
    }

    @Test
    void 승인된_발주_requester와_approver_batch_조회됨() {
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(approvedOrder());
        given(loadUserInfoPort.loadByUserCodes(argThat(codes ->
                codes.contains(REQUESTER_CODE) && codes.contains(APPROVER_CODE) && codes.size() == 2
        ))).willReturn(Map.of(
                REQUESTER_CODE, new UserInfo(REQUESTER_CODE, "정유진", "서비스 매니저"),
                APPROVER_CODE, new UserInfo(APPROVER_CODE, "강지석", "본사 매니저")
        ));

        HqSalesOrderDetail result = service.get(query(UserRole.HQ_MANAGER));

        assertThat(result.requesterInfo().name()).isEqualTo("정유진");
        assertThat(result.approverInfo().name()).isEqualTo("강지석");
    }

    @Test
    void requester와_approver_동일인이면_user_서비스_1회만_호출됨() {
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(selfApprovedOrder());
        given(loadUserInfoPort.loadByUserCodes(List.of(REQUESTER_CODE)))
                .willReturn(Map.of(REQUESTER_CODE, new UserInfo(REQUESTER_CODE, "정유진", "서비스 매니저")));

        service.get(query(UserRole.HQ_MANAGER));

        then(loadUserInfoPort).should().loadByUserCodes(List.of(REQUESTER_CODE));
    }

    @Test
    void 미승인_발주_approverInfo_null() {
        HqSalesOrderDetail result = service.get(query(UserRole.HQ_MANAGER));

        assertThat(result.approverInfo()).isNull();
    }

    @Test
    void 창고명이_응답에_올바르게_매핑됨() {
        HqSalesOrderDetail result = service.get(query(UserRole.HQ_MANAGER));

        assertThat(result.fromWarehouseName()).isEqualTo(FROM_WAREHOUSE_NAME);
        assertThat(result.toWarehouseName()).isEqualTo(TO_WAREHOUSE_NAME);
    }

    private GetHqSalesOrderDetailQuery query(UserRole role) {
        return new GetHqSalesOrderDetailQuery(SO_CODE, role);
    }

    private SalesOrder requestedOrder() {
        return new SalesOrder(
                SO_CODE, FROM_WAREHOUSE, TO_WAREHOUSE,
                SalesOrderStatus.REQUESTED, FIXED_DATE.plusDays(3), null,
                new SalesOrderCreation(REQUESTER_CODE, FIXED_INSTANT),
                new SalesOrderRequest(REQUESTER_CODE, FIXED_INSTANT),
                null, null, null, null, List.of()
        );
    }

    private SalesOrder orderWithoutToWarehouse() {
        return new SalesOrder(
                SO_CODE, FROM_WAREHOUSE, null,
                SalesOrderStatus.DRAFT, FIXED_DATE.plusDays(3), null,
                new SalesOrderCreation(REQUESTER_CODE, FIXED_INSTANT),
                null, null, null, null, null, List.of()
        );
    }

    private SalesOrder approvedOrder() {
        return new SalesOrder(
                SO_CODE, FROM_WAREHOUSE, TO_WAREHOUSE,
                SalesOrderStatus.APPROVED, FIXED_DATE.plusDays(3), null,
                new SalesOrderCreation(REQUESTER_CODE, FIXED_INSTANT),
                new SalesOrderRequest(REQUESTER_CODE, FIXED_INSTANT),
                new SalesOrderApproval(APPROVER_CODE, FIXED_INSTANT, "TRUCK", "INV-001"),
                null, null, null, List.of()
        );
    }

    private SalesOrder selfApprovedOrder() {
        return new SalesOrder(
                SO_CODE, FROM_WAREHOUSE, TO_WAREHOUSE,
                SalesOrderStatus.APPROVED, FIXED_DATE.plusDays(3), null,
                new SalesOrderCreation(REQUESTER_CODE, FIXED_INSTANT),
                new SalesOrderRequest(REQUESTER_CODE, FIXED_INSTANT),
                new SalesOrderApproval(REQUESTER_CODE, FIXED_INSTANT, "TRUCK", "INV-001"),
                null, null, null, List.of()
        );
    }
}
