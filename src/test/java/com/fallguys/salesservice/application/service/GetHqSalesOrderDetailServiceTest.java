package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.query.GetHqSalesOrderDetailQuery;
import com.fallguys.salesservice.application.port.inbound.model.SalesOrderDetail;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderStatusHistoryPort;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.ResourceNotFoundException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.model.ActorRef;
import com.fallguys.salesservice.domain.model.UserRole;
import com.fallguys.salesservice.domain.model.WarehouseRef;
import com.fallguys.salesservice.domain.model.salesorder.SagaStatus;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderCreation;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderRequest;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.salesorderhistory.SalesOrderStatusHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GetHqSalesOrderDetailServiceTest {

    @Mock LoadSalesOrderPort loadSalesOrderPort;
    @Mock LoadSalesOrderStatusHistoryPort loadHistoryPort;

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

    private static final ActorRef REQUESTER = ActorRef.of(REQUESTER_CODE, "정유진", "서비스 매니저");
    private static final ActorRef APPROVER = ActorRef.of(APPROVER_CODE, "강지석", "본사 매니저");

    @BeforeEach
    void setUp() {
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(requestedOrder());
        given(loadHistoryPort.findLatestBySoCodeAndStatus(SO_CODE, SalesOrderStatus.APPROVED))
                .willReturn(Optional.empty());
    }

    @Test
    void HQ_MANAGER_상세_조회_성공() {
        SalesOrderDetail result = service.get(query(UserRole.HQ_MANAGER));

        assertThat(result.salesOrder().getCode()).isEqualTo(SO_CODE);
        assertThat(result.fromWarehouseName()).isEqualTo(FROM_WAREHOUSE_NAME);
        assertThat(result.toWarehouseName()).isEqualTo(TO_WAREHOUSE_NAME);
    }

    @Test
    void HQ_STAFF_상세_조회_성공() {
        assertThat(service.get(query(UserRole.HQ_STAFF)).salesOrder().getCode()).isEqualTo(SO_CODE);
    }

    @Test
    void ADMIN_상세_조회_성공() {
        assertThat(service.get(query(UserRole.ADMIN)).salesOrder().getCode()).isEqualTo(SO_CODE);
    }

    @Test
    void BRANCH_MANAGER_조회_시도시_ForbiddenException() {
        assertThatThrownBy(() -> service.get(query(UserRole.BRANCH_MANAGER)))
                .isInstanceOf(ForbiddenException.class);
        then(loadSalesOrderPort).shouldHaveNoInteractions();
        then(loadHistoryPort).shouldHaveNoInteractions();
    }

    @Test
    void BRANCH_STAFF_조회_시도시_ForbiddenException() {
        assertThatThrownBy(() -> service.get(query(UserRole.BRANCH_STAFF)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void SO_미존재시_ResourceNotFoundException() {
        given(loadSalesOrderPort.load(SO_CODE))
                .willThrow(new ResourceNotFoundException(SalesErrorCode.SO_NOT_FOUND));

        assertThatThrownBy(() -> service.get(query(UserRole.HQ_MANAGER)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void requester는_요청_스냅샷에서_반환됨() {
        SalesOrderDetail result = service.get(query(UserRole.HQ_MANAGER));

        assertThat(result.requester()).isNotNull();
        assertThat(result.requester().nameSnapshot()).isEqualTo("정유진");
        assertThat(result.requester().positionSnapshot()).isEqualTo("서비스 매니저");
    }

    @Test
    void approver는_APPROVED_이력_actor_스냅샷에서_반환됨() {
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(approvedOrder());
        given(loadHistoryPort.findLatestBySoCodeAndStatus(SO_CODE, SalesOrderStatus.APPROVED))
                .willReturn(Optional.of(SalesOrderStatusHistory.of(
                        SO_CODE, SalesOrderStatus.APPROVED, APPROVER, FIXED_INSTANT)));

        SalesOrderDetail result = service.get(query(UserRole.HQ_MANAGER));

        assertThat(result.requester().nameSnapshot()).isEqualTo("정유진");
        assertThat(result.approver().nameSnapshot()).isEqualTo("강지석");
    }

    @Test
    void 미승인_발주_approver_null() {
        assertThat(service.get(query(UserRole.HQ_MANAGER)).approver()).isNull();
    }

    @Test
    void 창고명이_스냅샷에서_매핑됨() {
        SalesOrderDetail result = service.get(query(UserRole.HQ_MANAGER));

        assertThat(result.fromWarehouseName()).isEqualTo(FROM_WAREHOUSE_NAME);
        assertThat(result.toWarehouseName()).isEqualTo(TO_WAREHOUSE_NAME);
    }

    private GetHqSalesOrderDetailQuery query(UserRole role) {
        return new GetHqSalesOrderDetailQuery(SO_CODE, role);
    }

    private SalesOrder requestedOrder() {
        return order(SalesOrderStatus.REQUESTED);
    }

    private SalesOrder approvedOrder() {
        return order(SalesOrderStatus.APPROVED);
    }

    private SalesOrder order(SalesOrderStatus status) {
        return new SalesOrder(
                SO_CODE,
                WarehouseRef.of(FROM_WAREHOUSE, FROM_WAREHOUSE_NAME),
                WarehouseRef.of(TO_WAREHOUSE, TO_WAREHOUSE_NAME),
                status, SagaStatus.NONE, null,
                new SalesOrderCreation(REQUESTER, FIXED_INSTANT),
                new SalesOrderRequest(REQUESTER, FIXED_INSTANT),
                List.of()
        );
    }
}
