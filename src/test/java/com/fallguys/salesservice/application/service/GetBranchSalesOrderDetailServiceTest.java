package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.query.GetBranchSalesOrderDetailQuery;
import com.fallguys.salesservice.application.port.inbound.model.SalesOrderDetail;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderStatusHistoryPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadWarehousePort;
import com.fallguys.salesservice.application.port.outbound.model.WarehouseInfo;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GetBranchSalesOrderDetailServiceTest {

    @Mock LoadSalesOrderPort loadSalesOrderPort;
    @Mock LoadSalesOrderStatusHistoryPort loadHistoryPort;
    @Mock LoadWarehousePort loadWarehousePort;

    @InjectMocks
    GetBranchSalesOrderDetailService service;

    private static final String SO_CODE = "SO-2026-06-0001";
    private static final String USER_CODE = "branch001";
    private static final String FROM_WAREHOUSE = "WH-BRANCH-01";
    private static final String FROM_WAREHOUSE_NAME = "강남 1지점";
    private static final String TO_WAREHOUSE = "WH-HQ-01";
    private static final String TO_WAREHOUSE_NAME = "본사 중앙창고";

    private static final ActorRef ACTOR = ActorRef.of(USER_CODE, "정유진", "지점 담당");

    @BeforeEach
    void setUp() {
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(requestedOrder(FROM_WAREHOUSE, TO_WAREHOUSE));
        given(loadHistoryPort.findLatestBySoCodeAndStatus(SO_CODE, SalesOrderStatus.APPROVED))
                .willReturn(Optional.empty());
        given(loadWarehousePort.load(FROM_WAREHOUSE)).willReturn(new WarehouseInfo(FROM_WAREHOUSE, FROM_WAREHOUSE_NAME));
        given(loadWarehousePort.load(TO_WAREHOUSE)).willReturn(new WarehouseInfo(TO_WAREHOUSE, TO_WAREHOUSE_NAME));
    }

    @Test
    void MANAGER_상세_조회_성공() {
        SalesOrderDetail result = service.get(query(UserRole.BRANCH_MANAGER));

        assertThat(result.salesOrder().getCode()).isEqualTo(SO_CODE);
        assertThat(result.fromWarehouseName()).isEqualTo(FROM_WAREHOUSE_NAME);
        assertThat(result.toWarehouseName()).isEqualTo(TO_WAREHOUSE_NAME);
    }

    @Test
    void STAFF_상세_조회_성공() {
        SalesOrderDetail result = service.get(query(UserRole.BRANCH_STAFF));

        assertThat(result.salesOrder().getCode()).isEqualTo(SO_CODE);
        assertThat(result.requester().nameSnapshot()).isEqualTo("정유진");
    }

    @Test
    void HQ_역할_조회_시도시_ForbiddenException() {
        assertThatThrownBy(() -> service.get(query(UserRole.HQ_MANAGER)))
                .isInstanceOf(ForbiddenException.class);
        then(loadSalesOrderPort).shouldHaveNoInteractions();
        then(loadHistoryPort).shouldHaveNoInteractions();
    }

    @Test
    void SO_미존재시_ResourceNotFoundException() {
        given(loadSalesOrderPort.load(SO_CODE))
                .willThrow(new ResourceNotFoundException(SalesErrorCode.SO_NOT_FOUND));

        assertThatThrownBy(() -> service.get(query(UserRole.BRANCH_STAFF)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void 소속_창고_불일치시_ForbiddenException() {
        assertThatThrownBy(() -> service.get(
                new GetBranchSalesOrderDetailQuery(SO_CODE, USER_CODE, UserRole.BRANCH_MANAGER, "WH-BRANCH-99")))
                .isInstanceOf(ForbiddenException.class);
        then(loadWarehousePort).shouldHaveNoInteractions();
    }

    @Test
    void 창고_code와_name이_매핑됨() {
        SalesOrderDetail result = service.get(query(UserRole.BRANCH_MANAGER));

        assertThat(result.salesOrder().getFrom().code()).isEqualTo(FROM_WAREHOUSE);
        assertThat(result.fromWarehouseName()).isEqualTo(FROM_WAREHOUSE_NAME);
        assertThat(result.salesOrder().getTo().code()).isEqualTo(TO_WAREHOUSE);
        assertThat(result.toWarehouseName()).isEqualTo(TO_WAREHOUSE_NAME);
    }

    private GetBranchSalesOrderDetailQuery query(UserRole role) {
        return new GetBranchSalesOrderDetailQuery(SO_CODE, USER_CODE, role, FROM_WAREHOUSE);
    }

    private SalesOrder requestedOrder(String fromCode, String toCode) {
        return new SalesOrder(
                SO_CODE, WarehouseRef.of(fromCode, null), WarehouseRef.of(toCode, null),
                SalesOrderStatus.REQUESTED, SagaStatus.NONE, null,
                new SalesOrderCreation(ACTOR, Instant.now()),
                new SalesOrderRequest(ACTOR, Instant.now()),
                List.of()
        );
    }
}
