package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.CreateSalesOrderLineCommand;
import com.fallguys.salesservice.application.port.inbound.SubmitSalesOrderCommand;
import com.fallguys.salesservice.application.port.outbound.*;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.ResourceNotFoundException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.exception.InvalidStatusTransitionException;
import com.fallguys.salesservice.domain.exception.SalesOrderException;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubmitSalesOrderServiceTest {

    @Mock LoadSalesOrderPort loadSalesOrderPort;
    @Mock VerifyWarehousePort verifyWarehousePort;
    @Mock LoadItemPort loadItemPort;
    @Mock SaveSalesOrderPort saveSalesOrderPort;

    @InjectMocks
    SubmitSalesOrderService service;

    private static final String SO_CODE = "SO-2026-06-0001";
    private static final String USER_CODE = "branch001";
    private static final String FROM_WAREHOUSE = "WH-BRANCH-01";
    private static final String TO_WAREHOUSE = "WH-HQ-01";
    private static final String DRAFT_TO_WAREHOUSE = "WH-HQ-DRAFT";
    private static final LocalDate VALID_DATE = LocalDate.now().plusDays(3);
    private static final LocalDate DRAFT_DATE = LocalDate.now().plusDays(10);

    @BeforeEach
    void setUp() {
        SalesOrder draftSalesOrder = new SalesOrder(
                SO_CODE, FROM_WAREHOUSE, DRAFT_TO_WAREHOUSE,
                SalesOrderStatus.DRAFT, DRAFT_DATE, null,
                new SalesOrderCreation(USER_CODE, Instant.now()),
                null, null, null, null, null,
                List.of(new SalesOrderLine(1L, SO_CODE, "ITEM-01", null, null, 2, null, null, Priority.NORMAL))
        );

        given(loadSalesOrderPort.load(SO_CODE)).willReturn(draftSalesOrder);
        given(loadItemPort.loadAll(any())).willReturn(
                Map.of("ITEM-01", new ItemInfo("ITEM-01", "브레이크패드", "EA"))
        );
        given(saveSalesOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void submit_success() {
        SubmitSalesOrderCommand command = command(
                List.of(new CreateSalesOrderLineCommand("ITEM-01", 5, Priority.NORMAL))
        );

        SalesOrder result = service.submit(command);

        assertThat(result.getStatus()).isEqualTo(SalesOrderStatus.REQUESTED);
        assertThat(result.getRequest()).isNotNull();
        assertThat(result.getRequest().requestedBy()).isEqualTo(USER_CODE);
        assertThat(result.getLines()).hasSize(1);
        assertThat(result.getLines().getFirst().getItemNameSnapshot()).isEqualTo("브레이크패드");
        assertThat(result.getToWarehouseCode()).isEqualTo(TO_WAREHOUSE).isNotEqualTo(DRAFT_TO_WAREHOUSE);
        assertThat(result.getDesiredArrivalDate()).isEqualTo(VALID_DATE).isNotEqualTo(DRAFT_DATE);

        then(verifyWarehousePort).should().verify(FROM_WAREHOUSE);
        then(verifyWarehousePort).should().verify(TO_WAREHOUSE);
    }

    @Test
    void submit_soNotFound_throwsResourceNotFoundException() {
        given(loadSalesOrderPort.load(SO_CODE))
                .willThrow(new ResourceNotFoundException(SalesErrorCode.SO_NOT_FOUND));

        assertThatThrownBy(() -> service.submit(command(List.of(
                new CreateSalesOrderLineCommand("ITEM-01", 1, Priority.NORMAL)
        )))).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void submit_notDraft_throwsSalesOrderException() {
        SalesOrder requestedOrder = new SalesOrder(
                SO_CODE, FROM_WAREHOUSE, TO_WAREHOUSE,
                SalesOrderStatus.REQUESTED, VALID_DATE, null,
                new SalesOrderCreation(USER_CODE, Instant.now()),
                new SalesOrderRequest(USER_CODE, Instant.now()),
                null, null, null, null, List.of()
        );
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(requestedOrder);

        assertThatThrownBy(() -> service.submit(command(List.of(
                new CreateSalesOrderLineCommand("ITEM-01", 1, Priority.NORMAL)
        )))).isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void submit_duplicateItem_throwsSalesOrderException() {
        assertThatThrownBy(() -> service.submit(command(List.of(
                new CreateSalesOrderLineCommand("ITEM-01", 1, Priority.NORMAL),
                new CreateSalesOrderLineCommand("ITEM-01", 2, Priority.NORMAL)
        )))).isInstanceOf(SalesOrderException.class)
                .hasMessageContaining("ITEM-01");
    }

    @Test
    void submit_desiredArrivalDateToday_throwsSalesOrderException() {
        SubmitSalesOrderCommand command = new SubmitSalesOrderCommand(
                SO_CODE, USER_CODE, UserRole.BRANCH_STAFF, FROM_WAREHOUSE, TO_WAREHOUSE, LocalDate.now(), null,
                List.of(new CreateSalesOrderLineCommand("ITEM-01", 1, Priority.NORMAL))
        );

        assertThatThrownBy(() -> service.submit(command))
                .isInstanceOf(SalesOrderException.class);
    }

    @Test
    void submit_desiredArrivalDateOver60Days_throwsSalesOrderException() {
        SubmitSalesOrderCommand command = new SubmitSalesOrderCommand(
                SO_CODE, USER_CODE, UserRole.BRANCH_STAFF, FROM_WAREHOUSE, TO_WAREHOUSE, LocalDate.now().plusDays(61), null,
                List.of(new CreateSalesOrderLineCommand("ITEM-01", 1, Priority.NORMAL))
        );

        assertThatThrownBy(() -> service.submit(command))
                .isInstanceOf(SalesOrderException.class);
    }

    @Test
    void submit_warehouseMismatch_throwsForbiddenException() {
        SubmitSalesOrderCommand command = new SubmitSalesOrderCommand(
                SO_CODE, USER_CODE, UserRole.BRANCH_STAFF, "WH-BRANCH-99", TO_WAREHOUSE, VALID_DATE, null,
                List.of(new CreateSalesOrderLineCommand("ITEM-01", 1, Priority.NORMAL))
        );

        assertThatThrownBy(() -> service.submit(command))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void submit_fromWarehouseNotFound_throwsResourceNotFoundException() {
        willThrow(new ResourceNotFoundException(SalesErrorCode.WAREHOUSE_NOT_FOUND))
                .given(verifyWarehousePort).verify(FROM_WAREHOUSE);

        assertThatThrownBy(() -> service.submit(command(List.of(
                new CreateSalesOrderLineCommand("ITEM-01", 1, Priority.NORMAL)
        )))).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void submit_fromWarehouseInactive_throwsSalesOrderException() {
        willThrow(new SalesOrderException(SalesErrorCode.WAREHOUSE_INACTIVE))
                .given(verifyWarehousePort).verify(FROM_WAREHOUSE);

        assertThatThrownBy(() -> service.submit(command(List.of(
                new CreateSalesOrderLineCommand("ITEM-01", 1, Priority.NORMAL)
        )))).isInstanceOf(SalesOrderException.class);
    }

    @Test
    void submit_toWarehouseNotFound_throwsResourceNotFoundException() {
        willThrow(new ResourceNotFoundException(SalesErrorCode.WAREHOUSE_NOT_FOUND))
                .given(verifyWarehousePort).verify(TO_WAREHOUSE);

        assertThatThrownBy(() -> service.submit(command(List.of(
                new CreateSalesOrderLineCommand("ITEM-01", 1, Priority.NORMAL)
        )))).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void submit_toWarehouseInactive_throwsSalesOrderException() {
        willThrow(new SalesOrderException(SalesErrorCode.WAREHOUSE_INACTIVE))
                .given(verifyWarehousePort).verify(TO_WAREHOUSE);

        assertThatThrownBy(() -> service.submit(command(List.of(
                new CreateSalesOrderLineCommand("ITEM-01", 1, Priority.NORMAL)
        )))).isInstanceOf(SalesOrderException.class);
    }

    @Test
    void submit_itemNotFound_throwsResourceNotFoundException() {
        given(loadItemPort.loadAll(any()))
                .willThrow(new ResourceNotFoundException(SalesErrorCode.ITEM_NOT_FOUND));

        assertThatThrownBy(() -> service.submit(command(List.of(
                new CreateSalesOrderLineCommand("ITEM-99", 1, Priority.NORMAL)
        )))).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void submit_itemInactive_throwsSalesOrderException() {
        given(loadItemPort.loadAll(any()))
                .willThrow(new SalesOrderException(SalesErrorCode.ITEM_INACTIVE));

        assertThatThrownBy(() -> service.submit(command(List.of(
                new CreateSalesOrderLineCommand("ITEM-01", 1, Priority.NORMAL)
        )))).isInstanceOf(SalesOrderException.class);
    }

    private SubmitSalesOrderCommand command(List<CreateSalesOrderLineCommand> lines) {
        return new SubmitSalesOrderCommand(SO_CODE, USER_CODE, UserRole.BRANCH_STAFF, FROM_WAREHOUSE, TO_WAREHOUSE, VALID_DATE, null, lines);
    }
}
