package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.CreateSalesOrderCommand;
import com.fallguys.salesservice.application.port.inbound.CreateSalesOrderLineCommand;
import com.fallguys.salesservice.application.port.outbound.*;
import com.fallguys.salesservice.domain.exception.ResourceNotFoundException;
import com.fallguys.salesservice.domain.exception.SalesOrderException;
import com.fallguys.salesservice.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CreateSalesOrderServiceTest {

    @Mock VerifyWarehousePort verifyWarehousePort;
    @Mock LoadItemPort loadItemPort;
    @Mock GenerateSoCodePort generateSoCodePort;
    @Mock SaveSalesOrderPort saveSalesOrderPort;

    @InjectMocks
    CreateSalesOrderService service;

    private static final String USER_CODE = "branch001";
    private static final String FROM_WAREHOUSE = "WH-BRANCH-01";
    private static final String TO_WAREHOUSE = "WH-HQ-01";
    private static final String SO_CODE = "SO-2026-06-00001";
    private static final LocalDate VALID_DATE = LocalDate.now().plusDays(3);

    @BeforeEach
    void setUp() {
        given(generateSoCodePort.generate()).willReturn(SO_CODE);
        given(saveSalesOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void create_requested_success() {
        given(loadItemPort.loadAll(List.of("ITEM-01")))
                .willReturn(Map.of("ITEM-01", new ItemInfo("ITEM-01", "브레이크패드", "EA")));

        CreateSalesOrderCommand command = requestedCommand(
                List.of(new CreateSalesOrderLineCommand("ITEM-01", 5, Priority.NORMAL))
        );

        SalesOrder result = service.create(command);

        ArgumentCaptor<SalesOrder> captor = ArgumentCaptor.forClass(SalesOrder.class);
        then(saveSalesOrderPort).should().save(captor.capture());
        SalesOrder saved = captor.getValue();

        assertThat(saved.getCode()).isEqualTo(SO_CODE);
        assertThat(saved.getFromWarehouseCode()).isEqualTo(FROM_WAREHOUSE);
        assertThat(saved.getToWarehouseCode()).isEqualTo(TO_WAREHOUSE);
        assertThat(saved.getStatus()).isEqualTo(SalesOrderStatus.REQUESTED);
        assertThat(saved.getRequest()).isNotNull();
        assertThat(saved.getLines()).hasSize(1);
        assertThat(saved.getLines().getFirst().getItemNameSnapshot()).isEqualTo("브레이크패드");
        assertThat(result).isSameAs(saved);
    }

    @Test
    void create_draft_success() {
        CreateSalesOrderCommand command = draftCommand(
                List.of(new CreateSalesOrderLineCommand("ITEM-01", 2, Priority.URGENT))
        );

        SalesOrder result = service.create(command);

        then(verifyWarehousePort).should(never()).verify(any());
        then(loadItemPort).should(never()).loadAll(any());

        assertThat(result.getStatus()).isEqualTo(SalesOrderStatus.DRAFT);
        assertThat(result.getRequest()).isNull();
        assertThat(result.getLines().getFirst().getItemNameSnapshot()).isNull();
    }

    @Test
    void create_duplicateItem_throwsSalesOrderException() {
        CreateSalesOrderCommand command = requestedCommand(List.of(
                new CreateSalesOrderLineCommand("ITEM-01", 1, Priority.NORMAL),
                new CreateSalesOrderLineCommand("ITEM-01", 2, Priority.NORMAL)
        ));

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(SalesOrderException.class)
                .hasMessageContaining("ITEM-01");
    }

    @Test
    void create_desiredArrivalDateToday_throwsSalesOrderException() {
        CreateSalesOrderCommand command = new CreateSalesOrderCommand(
                FROM_WAREHOUSE, TO_WAREHOUSE, LocalDate.now(), null, SalesOrderStatus.REQUESTED,
                List.of(new CreateSalesOrderLineCommand("ITEM-01", 1, Priority.NORMAL)),
                USER_CODE, UserRole.BRANCH_STAFF
        );

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(SalesOrderException.class);
    }

    @Test
    void create_desiredArrivalDateOver60Days_throwsSalesOrderException() {
        CreateSalesOrderCommand command = new CreateSalesOrderCommand(
                FROM_WAREHOUSE, TO_WAREHOUSE, LocalDate.now().plusDays(61), null, SalesOrderStatus.REQUESTED,
                List.of(new CreateSalesOrderLineCommand("ITEM-01", 1, Priority.NORMAL)),
                USER_CODE, UserRole.BRANCH_STAFF
        );

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(SalesOrderException.class);
    }

    @Test
    void create_warehouseNotFound_throwsResourceNotFoundException() {
        given(loadItemPort.loadAll(any()))
                .willReturn(Map.of("ITEM-01", new ItemInfo("ITEM-01", "브레이크패드", "EA")));
        willThrow(new ResourceNotFoundException(com.fallguys.salesservice.domain.exception.SalesErrorCode.WAREHOUSE_NOT_FOUND))
                .given(verifyWarehousePort).verify(TO_WAREHOUSE);

        CreateSalesOrderCommand command = requestedCommand(
                List.of(new CreateSalesOrderLineCommand("ITEM-01", 1, Priority.NORMAL))
        );

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_itemNotFound_throwsResourceNotFoundException() {
        given(loadItemPort.loadAll(any()))
                .willThrow(new ResourceNotFoundException(com.fallguys.salesservice.domain.exception.SalesErrorCode.ITEM_NOT_FOUND));

        CreateSalesOrderCommand command = requestedCommand(
                List.of(new CreateSalesOrderLineCommand("ITEM-99", 1, Priority.NORMAL))
        );

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_draft_emptyLines_success() {
        CreateSalesOrderCommand command = draftCommand(List.of());

        SalesOrder result = service.create(command);

        assertThat(result.getLines()).isEmpty();
    }

    private CreateSalesOrderCommand requestedCommand(List<CreateSalesOrderLineCommand> lines) {
        return new CreateSalesOrderCommand(FROM_WAREHOUSE, TO_WAREHOUSE, VALID_DATE, null, SalesOrderStatus.REQUESTED, lines, USER_CODE, UserRole.BRANCH_STAFF);
    }

    private CreateSalesOrderCommand draftCommand(List<CreateSalesOrderLineCommand> lines) {
        return new CreateSalesOrderCommand(FROM_WAREHOUSE, TO_WAREHOUSE, VALID_DATE, null, SalesOrderStatus.DRAFT, lines, USER_CODE, UserRole.BRANCH_STAFF);
    }
}
