package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.command.RequestSalesOrderCommand;
import com.fallguys.salesservice.application.port.outbound.model.ItemInfo;
import com.fallguys.salesservice.application.port.outbound.port.LoadItemPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadWarehousePort;
import com.fallguys.salesservice.application.port.outbound.model.WarehouseInfo;
import com.fallguys.salesservice.application.port.outbound.port.SaveSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.AppendSalesOrderStatusHistoryPort;
import com.fallguys.salesservice.application.port.outbound.port.VerifyWarehousePort;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.InvalidStatusTransitionException;
import com.fallguys.salesservice.domain.exception.ResourceNotFoundException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.exception.SalesOrderException;
import com.fallguys.salesservice.domain.model.*;
import com.fallguys.salesservice.domain.model.salesorder.SagaStatus;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderCreation;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderRequest;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RequestSalesOrderServiceTest {

    @Mock
    LoadSalesOrderPort loadSalesOrderPort;
    @Mock
    VerifyWarehousePort verifyWarehousePort;
    @Mock
    LoadWarehousePort loadWarehousePort;
    @Mock
    LoadItemPort loadItemPort;
    @Mock
    SaveSalesOrderPort saveSalesOrderPort;
    @Mock
    AppendSalesOrderStatusHistoryPort appendHistoryPort;

    @Mock
    UserActivityRecorder userActivityRecorder;

    @InjectMocks
    RequestSalesOrderService service;

    private static final String SO_CODE = "SO-2026-06-0001";
    private static final String USER_CODE = "branch001";
    private static final String USER_NAME = "정유진";
    private static final String USER_POSITION = "지점 담당";
    private static final String FROM_WAREHOUSE = "WH-BRANCH-01";
    private static final String TO_WAREHOUSE = "WH-HQ-01";
    private static final ActorRef ACTOR = ActorRef.of(USER_CODE, USER_NAME, USER_POSITION);

    @BeforeEach
    void setUp() {
        SalesOrder draftSalesOrder = new SalesOrder(
                SO_CODE, WarehouseRef.of(FROM_WAREHOUSE, null), WarehouseRef.of(TO_WAREHOUSE, null),
                SalesOrderStatus.DRAFT, SagaStatus.NONE, null,
                new SalesOrderCreation(ACTOR, Instant.now()),
                null,
                List.of(new SalesOrderLine(1L, SO_CODE, "ITEM-01", null, null, 2, Priority.NORMAL)),
                null
        );

        given(loadSalesOrderPort.load(SO_CODE)).willReturn(draftSalesOrder);
        given(loadItemPort.loadAll(any())).willReturn(
                Map.of("ITEM-01", new ItemInfo("ITEM-01", "브레이크패드", "EA"))
        );
        given(loadWarehousePort.load(FROM_WAREHOUSE)).willReturn(new WarehouseInfo(FROM_WAREHOUSE, "강남 1지점"));
        given(loadWarehousePort.load(TO_WAREHOUSE)).willReturn(new WarehouseInfo(TO_WAREHOUSE, "본사"));
        given(saveSalesOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void request_success() {
        SalesOrder result = service.request(command());

        assertThat(result.getStatus()).isEqualTo(SalesOrderStatus.REQUESTED);
        assertThat(result.getRequest()).isNotNull();
        assertThat(result.getRequest().requestedBy().code()).isEqualTo(USER_CODE);
        assertThat(result.getLines()).hasSize(1);
        assertThat(result.getLines().getFirst().getItemNameSnapshot()).isEqualTo("브레이크패드");
        assertThat(result.getLines().getFirst().getUnitSnapshot()).isEqualTo("EA");
        assertThat(result.getTo().code()).isEqualTo(TO_WAREHOUSE);

        then(verifyWarehousePort).should().verify(FROM_WAREHOUSE);
        then(verifyWarehousePort).should().verify(TO_WAREHOUSE);

        then(appendHistoryPort).should().append(argThat(h ->
                h.status() == SalesOrderStatus.REQUESTED &&
                h.actor().code().equals(USER_CODE) &&
                h.payload() == null));
    }

    @Test
    void request_hqRole_throwsForbiddenException() {
        RequestSalesOrderCommand command = new RequestSalesOrderCommand(
                SO_CODE, USER_CODE, USER_NAME, USER_POSITION, UserRole.HQ_MANAGER, FROM_WAREHOUSE);

        assertThatThrownBy(() -> service.request(command))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void request_soNotFound_throwsResourceNotFoundException() {
        given(loadSalesOrderPort.load(SO_CODE))
                .willThrow(new ResourceNotFoundException(SalesErrorCode.SO_NOT_FOUND));

        assertThatThrownBy(() -> service.request(command()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void request_warehouseMismatch_throwsForbiddenException() {
        RequestSalesOrderCommand command = new RequestSalesOrderCommand(
                SO_CODE, USER_CODE, USER_NAME, USER_POSITION, UserRole.BRANCH_STAFF, "WH-BRANCH-99");

        assertThatThrownBy(() -> service.request(command))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void request_notDraft_throwsInvalidStatusTransitionException() {
        SalesOrder requestedOrder = new SalesOrder(
                SO_CODE, WarehouseRef.of(FROM_WAREHOUSE, null), WarehouseRef.of(TO_WAREHOUSE, null),
                SalesOrderStatus.REQUESTED, SagaStatus.NONE, null,
                new SalesOrderCreation(ACTOR, Instant.now()),
                new SalesOrderRequest(ACTOR, Instant.now()),
                List.of(),
                null
        );
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(requestedOrder);

        assertThatThrownBy(() -> service.request(command()))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void request_duplicateItems_throwsSalesOrderException() {
        SalesOrder draftWithDuplicates = new SalesOrder(
                SO_CODE, WarehouseRef.of(FROM_WAREHOUSE, null), WarehouseRef.of(TO_WAREHOUSE, null),
                SalesOrderStatus.DRAFT, SagaStatus.NONE, null,
                new SalesOrderCreation(ACTOR, Instant.now()),
                null,
                List.of(
                    new SalesOrderLine(1L, SO_CODE, "ITEM-01", null, null, 2, Priority.NORMAL),
                    new SalesOrderLine(2L, SO_CODE, "ITEM-01", null, null, 3, Priority.NORMAL)
                ),
                null
        );
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(draftWithDuplicates);

        assertThatThrownBy(() -> service.request(command()))
                .isInstanceOf(SalesOrderException.class)
                .hasFieldOrPropertyWithValue("code", SalesErrorCode.DUPLICATE_ITEM.getCode());
    }

    @Test
    void request_fromWarehouseNotFound_throwsResourceNotFoundException() {
        willThrow(new ResourceNotFoundException(SalesErrorCode.WAREHOUSE_NOT_FOUND))
                .given(verifyWarehousePort).verify(FROM_WAREHOUSE);

        assertThatThrownBy(() -> service.request(command()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void request_fromWarehouseInactive_throwsSalesOrderException() {
        willThrow(new SalesOrderException(SalesErrorCode.WAREHOUSE_INACTIVE))
                .given(verifyWarehousePort).verify(FROM_WAREHOUSE);

        assertThatThrownBy(() -> service.request(command()))
                .isInstanceOf(SalesOrderException.class);
    }

    @Test
    void request_toWarehouseNotFound_throwsResourceNotFoundException() {
        willThrow(new ResourceNotFoundException(SalesErrorCode.WAREHOUSE_NOT_FOUND))
                .given(verifyWarehousePort).verify(TO_WAREHOUSE);

        assertThatThrownBy(() -> service.request(command()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void request_toWarehouseInactive_throwsSalesOrderException() {
        willThrow(new SalesOrderException(SalesErrorCode.WAREHOUSE_INACTIVE))
                .given(verifyWarehousePort).verify(TO_WAREHOUSE);

        assertThatThrownBy(() -> service.request(command()))
                .isInstanceOf(SalesOrderException.class);
    }

    @Test
    void request_itemNotFound_throwsResourceNotFoundException() {
        given(loadItemPort.loadAll(any()))
                .willThrow(new ResourceNotFoundException(SalesErrorCode.ITEM_NOT_FOUND));

        assertThatThrownBy(() -> service.request(command()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void request_itemInactive_throwsSalesOrderException() {
        given(loadItemPort.loadAll(any()))
                .willThrow(new SalesOrderException(SalesErrorCode.ITEM_INACTIVE));

        assertThatThrownBy(() -> service.request(command()))
                .isInstanceOf(SalesOrderException.class);
    }

    private RequestSalesOrderCommand command() {
        return new RequestSalesOrderCommand(
                SO_CODE, USER_CODE, USER_NAME, USER_POSITION, UserRole.BRANCH_STAFF, FROM_WAREHOUSE);
    }
}
