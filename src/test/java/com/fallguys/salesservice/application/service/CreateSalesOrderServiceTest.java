package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.command.CreateSalesOrderCommand;
import com.fallguys.salesservice.application.port.inbound.command.CreateSalesOrderLineCommand;
import com.fallguys.salesservice.application.port.outbound.model.ItemInfo;
import com.fallguys.salesservice.application.port.outbound.port.GenerateSoCodePort;
import com.fallguys.salesservice.application.port.outbound.port.LoadItemPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadWarehousePort;
import com.fallguys.salesservice.application.port.outbound.model.WarehouseInfo;
import com.fallguys.salesservice.application.port.outbound.port.SaveSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.AppendSalesOrderStatusHistoryPort;
import com.fallguys.salesservice.application.port.outbound.port.VerifyWarehousePort;
import com.fallguys.salesservice.domain.exception.ResourceNotFoundException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.exception.SalesOrderException;
import com.fallguys.salesservice.domain.model.*;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.salesorderline.Priority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CreateSalesOrderServiceTest {

    @Mock
    VerifyWarehousePort verifyWarehousePort;
    @Mock
    LoadWarehousePort loadWarehousePort;
    @Mock
    LoadItemPort loadItemPort;
    @Mock
    GenerateSoCodePort generateSoCodePort;
    @Mock
    SaveSalesOrderPort saveSalesOrderPort;
    @Mock
    AppendSalesOrderStatusHistoryPort appendHistoryPort;

    @InjectMocks
    CreateSalesOrderService service;

    private static final String USER_CODE = "branch001";
    private static final String FROM_WAREHOUSE = "WH-BRANCH-01";
    private static final String TO_WAREHOUSE = "WH-HQ-01";
    private static final String SO_CODE = "SO-2026-06-00001";
    private static final String USER_NAME = "정유진";
    private static final String USER_POSITION = "지점 담당";

    @BeforeEach
    void setUp() {
        given(generateSoCodePort.generate()).willReturn(SO_CODE);
        given(loadWarehousePort.load(FROM_WAREHOUSE)).willReturn(new WarehouseInfo(FROM_WAREHOUSE, "강남 1지점"));
        given(loadWarehousePort.load(TO_WAREHOUSE)).willReturn(new WarehouseInfo(TO_WAREHOUSE, "본사"));
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
        assertThat(saved.getFrom().code()).isEqualTo(FROM_WAREHOUSE);
        assertThat(saved.getTo().code()).isEqualTo(TO_WAREHOUSE);
        assertThat(saved.getStatus()).isEqualTo(SalesOrderStatus.REQUESTED);
        assertThat(saved.getRequest()).isNotNull();
        assertThat(saved.getLines()).hasSize(1);
        assertThat(saved.getLines().getFirst().getItemNameSnapshot()).isEqualTo("브레이크패드");
        assertThat(result).isSameAs(saved);

        then(verifyWarehousePort).should().verify(FROM_WAREHOUSE);
        then(verifyWarehousePort).should().verify(TO_WAREHOUSE);

        then(appendHistoryPort).should().append(argThat(h ->
                h.status() == SalesOrderStatus.REQUESTED && h.payload() == null));
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

        then(appendHistoryPort).should().append(argThat(h ->
                h.status() == SalesOrderStatus.DRAFT && h.payload() == null));
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
    void create_fromWarehouseNotFound_throwsResourceNotFoundException() {
        willThrow(new ResourceNotFoundException(SalesErrorCode.WAREHOUSE_NOT_FOUND))
                .given(verifyWarehousePort).verify(FROM_WAREHOUSE);

        assertThatThrownBy(() -> service.create(requestedCommand(
                List.of(new CreateSalesOrderLineCommand("ITEM-01", 1, Priority.NORMAL))
        ))).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_fromWarehouseInactive_throwsSalesOrderException() {
        willThrow(new SalesOrderException(SalesErrorCode.WAREHOUSE_INACTIVE))
                .given(verifyWarehousePort).verify(FROM_WAREHOUSE);

        assertThatThrownBy(() -> service.create(requestedCommand(
                List.of(new CreateSalesOrderLineCommand("ITEM-01", 1, Priority.NORMAL))
        ))).isInstanceOf(SalesOrderException.class);
    }

    @Test
    void create_toWarehouseNotFound_throwsResourceNotFoundException() {
        willThrow(new ResourceNotFoundException(SalesErrorCode.WAREHOUSE_NOT_FOUND))
                .given(verifyWarehousePort).verify(TO_WAREHOUSE);

        assertThatThrownBy(() -> service.create(requestedCommand(
                List.of(new CreateSalesOrderLineCommand("ITEM-01", 1, Priority.NORMAL))
        ))).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_toWarehouseInactive_throwsSalesOrderException() {
        willThrow(new SalesOrderException(SalesErrorCode.WAREHOUSE_INACTIVE))
                .given(verifyWarehousePort).verify(TO_WAREHOUSE);

        assertThatThrownBy(() -> service.create(requestedCommand(
                List.of(new CreateSalesOrderLineCommand("ITEM-01", 1, Priority.NORMAL))
        ))).isInstanceOf(SalesOrderException.class);
    }

    @Test
    void create_itemNotFound_throwsResourceNotFoundException() {
        given(loadItemPort.loadAll(any()))
                .willThrow(new ResourceNotFoundException(SalesErrorCode.ITEM_NOT_FOUND));

        assertThatThrownBy(() -> service.create(requestedCommand(
                List.of(new CreateSalesOrderLineCommand("ITEM-99", 1, Priority.NORMAL))
        ))).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_itemInactive_throwsSalesOrderException() {
        given(loadItemPort.loadAll(any()))
                .willThrow(new SalesOrderException(SalesErrorCode.ITEM_INACTIVE));

        assertThatThrownBy(() -> service.create(requestedCommand(
                List.of(new CreateSalesOrderLineCommand("ITEM-01", 1, Priority.NORMAL))
        ))).isInstanceOf(SalesOrderException.class);
    }

    @Test
    void create_draft_emptyLines_success() {
        CreateSalesOrderCommand command = draftCommand(List.of());

        SalesOrder result = service.create(command);

        assertThat(result.getLines()).isEmpty();
    }

    private CreateSalesOrderCommand requestedCommand(List<CreateSalesOrderLineCommand> lines) {
        return new CreateSalesOrderCommand(FROM_WAREHOUSE, TO_WAREHOUSE, null, SalesOrderStatus.REQUESTED, lines,
                USER_CODE, USER_NAME, USER_POSITION, UserRole.BRANCH_STAFF);
    }

    private CreateSalesOrderCommand draftCommand(List<CreateSalesOrderLineCommand> lines) {
        return new CreateSalesOrderCommand(FROM_WAREHOUSE, TO_WAREHOUSE, null, SalesOrderStatus.DRAFT, lines,
                USER_CODE, USER_NAME, USER_POSITION, UserRole.BRANCH_STAFF);
    }
}
