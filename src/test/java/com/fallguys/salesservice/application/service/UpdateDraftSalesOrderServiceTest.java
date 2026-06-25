package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.command.CreateSalesOrderLineCommand;
import com.fallguys.salesservice.application.port.inbound.command.UpdateDraftSalesOrderCommand;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.SaveSalesOrderPort;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UpdateDraftSalesOrderServiceTest {

    @Mock LoadSalesOrderPort loadSalesOrderPort;
    @Mock SaveSalesOrderPort saveSalesOrderPort;

    @Mock UserActivityRecorder userActivityRecorder;

    @InjectMocks
    UpdateDraftSalesOrderService service;

    private static final String SO_CODE = "SO-2026-06-0001";
    private static final String USER_CODE = "branch001";
    private static final String USER_NAME = "정유진";
    private static final String USER_POSITION = "지점 담당";
    private static final String FROM_WAREHOUSE = "WH-BRANCH-01";
    private static final String OLD_TO_WAREHOUSE = "WH-HQ-OLD";
    private static final String NEW_TO_WAREHOUSE = "WH-HQ-NEW";
    private static final ActorRef ACTOR = ActorRef.of(USER_CODE, USER_NAME, USER_POSITION);

    @BeforeEach
    void setUp() {
        SalesOrder draftSalesOrder = new SalesOrder(
                SO_CODE, WarehouseRef.of(FROM_WAREHOUSE, null), WarehouseRef.of(OLD_TO_WAREHOUSE, null),
                SalesOrderStatus.DRAFT, SagaStatus.NONE, "old memo",
                new SalesOrderCreation(ACTOR, Instant.now()),
                null,
                List.of(new SalesOrderLine(1L, SO_CODE, "ITEM-01", null, null, 2, Priority.NORMAL)),
                null
        );

        given(loadSalesOrderPort.load(SO_CODE)).willReturn(draftSalesOrder);
        given(saveSalesOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void updateDraft_success_overwritesFieldsAndStaysDraft() {
        UpdateDraftSalesOrderCommand command = command(
                List.of(new CreateSalesOrderLineCommand("ITEM-02", 7, Priority.URGENT))
        );

        SalesOrder result = service.updateDraft(command);

        assertThat(result.getStatus()).isEqualTo(SalesOrderStatus.DRAFT);
        assertThat(result.getRequest()).isNull();
        assertThat(result.getTo().code()).isEqualTo(NEW_TO_WAREHOUSE);
        assertThat(result.getRequestMemo()).isEqualTo("new memo");
        assertThat(result.getLines()).hasSize(1);
        SalesOrderLine line = result.getLines().getFirst();
        assertThat(line.getItemCode()).isEqualTo("ITEM-02");
        assertThat(line.getQuantity()).isEqualTo(7);
        assertThat(line.getItemNameSnapshot()).isNull();
        assertThat(line.getUnitSnapshot()).isNull();
        then(saveSalesOrderPort).should().save(any());
    }

    @Test
    void updateDraft_emptyLines_success() {
        SalesOrder result = service.updateDraft(command(List.of()));

        assertThat(result.getStatus()).isEqualTo(SalesOrderStatus.DRAFT);
        assertThat(result.getLines()).isEmpty();
    }

    @Test
    void updateDraft_hqRole_throwsForbiddenException() {
        UpdateDraftSalesOrderCommand command = new UpdateDraftSalesOrderCommand(
                SO_CODE, USER_CODE, UserRole.HQ_MANAGER, FROM_WAREHOUSE,
                NEW_TO_WAREHOUSE, "new memo",
                List.of(new CreateSalesOrderLineCommand("ITEM-01", 1, Priority.NORMAL))
        );

        assertThatThrownBy(() -> service.updateDraft(command))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateDraft_soNotFound_throwsResourceNotFoundException() {
        given(loadSalesOrderPort.load(SO_CODE))
                .willThrow(new ResourceNotFoundException(SalesErrorCode.SO_NOT_FOUND));

        assertThatThrownBy(() -> service.updateDraft(command(List.of(
                new CreateSalesOrderLineCommand("ITEM-01", 1, Priority.NORMAL)
        )))).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateDraft_warehouseMismatch_throwsForbiddenException() {
        UpdateDraftSalesOrderCommand command = new UpdateDraftSalesOrderCommand(
                SO_CODE, USER_CODE, UserRole.BRANCH_STAFF, "WH-BRANCH-99",
                NEW_TO_WAREHOUSE, "new memo",
                List.of(new CreateSalesOrderLineCommand("ITEM-01", 1, Priority.NORMAL))
        );

        assertThatThrownBy(() -> service.updateDraft(command))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateDraft_notDraft_throwsInvalidStatusTransitionException() {
        SalesOrder requestedOrder = new SalesOrder(
                SO_CODE, WarehouseRef.of(FROM_WAREHOUSE, null), WarehouseRef.of(OLD_TO_WAREHOUSE, null),
                SalesOrderStatus.REQUESTED, SagaStatus.NONE, null,
                new SalesOrderCreation(ACTOR, Instant.now()),
                new SalesOrderRequest(ACTOR, Instant.now()),
                List.of(),
                null
        );
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(requestedOrder);

        assertThatThrownBy(() -> service.updateDraft(command(List.of(
                new CreateSalesOrderLineCommand("ITEM-01", 1, Priority.NORMAL)
        )))).isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void updateDraft_duplicateItem_throwsSalesOrderException() {
        assertThatThrownBy(() -> service.updateDraft(command(List.of(
                new CreateSalesOrderLineCommand("ITEM-01", 1, Priority.NORMAL),
                new CreateSalesOrderLineCommand("ITEM-01", 2, Priority.NORMAL)
        )))).isInstanceOf(SalesOrderException.class)
                .hasMessageContaining("ITEM-01");
    }

    private UpdateDraftSalesOrderCommand command(List<CreateSalesOrderLineCommand> lines) {
        return new UpdateDraftSalesOrderCommand(
                SO_CODE, USER_CODE, UserRole.BRANCH_STAFF, FROM_WAREHOUSE,
                NEW_TO_WAREHOUSE, "new memo", lines);
    }
}
