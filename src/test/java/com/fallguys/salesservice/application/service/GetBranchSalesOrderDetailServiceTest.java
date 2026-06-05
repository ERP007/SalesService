package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.GetBranchSalesOrderDetailQuery;
import com.fallguys.salesservice.application.port.inbound.SalesOrderDetail;
import com.fallguys.salesservice.application.port.outbound.BranchUserInfo;
import com.fallguys.salesservice.application.port.outbound.LoadBranchUserPort;
import com.fallguys.salesservice.application.port.outbound.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.LoadWarehousePort;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GetBranchSalesOrderDetailServiceTest {

    @Mock LoadSalesOrderPort loadSalesOrderPort;
    @Mock LoadBranchUserPort loadBranchUserPort;
    @Mock LoadWarehousePort loadWarehousePort;

    @InjectMocks
    GetBranchSalesOrderDetailService service;

    private static final String SO_CODE = "SO-2026-06-0001";
    private static final String USER_CODE = "branch001";
    private static final String FROM_WAREHOUSE = "WH-BRANCH-01";
    private static final String FROM_WAREHOUSE_NAME = "강남 1지점";
    private static final String TO_WAREHOUSE = "WH-HQ-01";
    private static final String TO_WAREHOUSE_NAME = "본사 중앙창고";

    @BeforeEach
    void setUp() {
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(requestedOrder(FROM_WAREHOUSE, TO_WAREHOUSE));
        given(loadBranchUserPort.load(USER_CODE)).willReturn(new BranchUserInfo(FROM_WAREHOUSE));
        given(loadWarehousePort.load(FROM_WAREHOUSE)).willReturn(new WarehouseInfo(FROM_WAREHOUSE, FROM_WAREHOUSE_NAME));
        given(loadWarehousePort.load(TO_WAREHOUSE)).willReturn(new WarehouseInfo(TO_WAREHOUSE, TO_WAREHOUSE_NAME));
    }

    @Test
    void MANAGER_상세_조회_성공() {
        SalesOrderDetail result = service.get(query(USER_CODE, UserRole.BRANCH_MANAGER));

        assertThat(result.salesOrder().getCode()).isEqualTo(SO_CODE);
        assertThat(result.fromWarehouseName()).isEqualTo(FROM_WAREHOUSE_NAME);
        assertThat(result.toWarehouseName()).isEqualTo(TO_WAREHOUSE_NAME);
    }

    @Test
    void STAFF_상세_조회_성공() {
        SalesOrderDetail result = service.get(query(USER_CODE, UserRole.BRANCH_STAFF));

        assertThat(result.salesOrder().getCode()).isEqualTo(SO_CODE);
        assertThat(result.fromWarehouseName()).isEqualTo(FROM_WAREHOUSE_NAME);
        assertThat(result.toWarehouseName()).isEqualTo(TO_WAREHOUSE_NAME);
    }

    @Test
    void toWarehouseCode_null이면_toWarehouseName_null_반환() {
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(requestedOrder(FROM_WAREHOUSE, null));

        SalesOrderDetail result = service.get(query(USER_CODE, UserRole.BRANCH_MANAGER));

        assertThat(result.toWarehouseName()).isNull();
        then(loadWarehousePort).should(never()).load(null);
    }

    @Test
    void HQ_역할_조회_시도시_ForbiddenException() {
        assertThatThrownBy(() -> service.get(query(USER_CODE, UserRole.HQ_MANAGER)))
                .isInstanceOf(ForbiddenException.class);

        then(loadSalesOrderPort).shouldHaveNoInteractions();
    }

    @Test
    void SO_미존재시_ResourceNotFoundException() {
        given(loadSalesOrderPort.load(SO_CODE))
                .willThrow(new ResourceNotFoundException(SalesErrorCode.SO_NOT_FOUND));

        assertThatThrownBy(() -> service.get(query(USER_CODE, UserRole.BRANCH_STAFF)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void 소속_창고_불일치시_ForbiddenException() {
        given(loadBranchUserPort.load(USER_CODE)).willReturn(new BranchUserInfo("WH-BRANCH-99"));

        assertThatThrownBy(() -> service.get(query(USER_CODE, UserRole.BRANCH_MANAGER)))
                .isInstanceOf(ForbiddenException.class);

        then(loadWarehousePort).shouldHaveNoInteractions();
    }

    @Test
    void 창고명이_응답에_올바르게_매핑됨() {
        SalesOrderDetail result = service.get(query(USER_CODE, UserRole.BRANCH_MANAGER));

        assertThat(result.salesOrder().getFromWarehouseCode()).isEqualTo(FROM_WAREHOUSE);
        assertThat(result.fromWarehouseName()).isEqualTo(FROM_WAREHOUSE_NAME);
        assertThat(result.salesOrder().getToWarehouseCode()).isEqualTo(TO_WAREHOUSE);
        assertThat(result.toWarehouseName()).isEqualTo(TO_WAREHOUSE_NAME);
    }

    private GetBranchSalesOrderDetailQuery query(String userCode, UserRole role) {
        return new GetBranchSalesOrderDetailQuery(SO_CODE, userCode, role);
    }

    private SalesOrder requestedOrder(String fromWarehouseCode, String toWarehouseCode) {
        return new SalesOrder(
                SO_CODE, fromWarehouseCode, toWarehouseCode,
                SalesOrderStatus.REQUESTED, LocalDate.now().plusDays(3), null,
                new SalesOrderCreation(USER_CODE, Instant.now()),
                new SalesOrderRequest(USER_CODE, Instant.now()),
                null, null, null, null, List.of()
        );
    }
}
