package com.fallguys.salesservice.domain.model.salesorder;

import com.fallguys.salesservice.domain.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SalesOrderTest {

    private SalesOrder requestedOrder() {
        return SalesOrder.create("SO-1", "WH-FROM", "WH-TO",
                SalesOrderStatus.REQUESTED, LocalDate.now(), "memo",
                "user", Instant.now(), List.of());
    }

    @Test
    void create_starts_with_saga_NONE() {
        SalesOrder order = requestedOrder();

        assertThat(order.getSagaStatus()).isEqualTo(SagaStatus.NONE);
    }

    @Test
    void approve_sets_APPROVED_and_saga_SENDING() {
        SalesOrder order = requestedOrder();

        order.approve();

        assertThat(order.getStatus()).isEqualTo(SalesOrderStatus.APPROVED);
        assertThat(order.getSagaStatus()).isEqualTo(SagaStatus.SENDING);
    }

    @Test
    void deliver_sets_DELIVERED_and_saga_SENDING() {
        SalesOrder order = requestedOrder();
        order.approve();
        order.markSagaProcessing();
        order.markSagaDone();

        order.deliver();

        assertThat(order.getStatus()).isEqualTo(SalesOrderStatus.DELIVERED);
        assertThat(order.getSagaStatus()).isEqualTo(SagaStatus.SENDING);
    }

    @Test
    void markSagaProcessing_transitions_SENDING_to_PROCESSING() {
        SalesOrder order = requestedOrder();
        order.approve();

        order.markSagaProcessing();

        assertThat(order.getSagaStatus()).isEqualTo(SagaStatus.PROCESSING);
    }

    @Test
    void markSagaProcessing_fails_when_not_SENDING() {
        SalesOrder order = requestedOrder();

        assertThatThrownBy(order::markSagaProcessing)
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void markSagaDone_transitions_PROCESSING_to_DONE() {
        SalesOrder order = requestedOrder();
        order.approve();
        order.markSagaProcessing();

        order.markSagaDone();

        assertThat(order.getSagaStatus()).isEqualTo(SagaStatus.DONE);
    }

    @Test
    void markSagaDone_fails_when_not_PROCESSING() {
        SalesOrder order = requestedOrder();
        order.approve();

        assertThatThrownBy(order::markSagaDone)
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void compensateApprove_reverts_to_REQUESTED_and_saga_FAILED() {
        SalesOrder order = requestedOrder();
        order.approve();
        order.markSagaProcessing();

        order.compensateApprove();

        assertThat(order.getStatus()).isEqualTo(SalesOrderStatus.REQUESTED);
        assertThat(order.getSagaStatus()).isEqualTo(SagaStatus.FAILED);
    }

    @Test
    void compensateApprove_fails_when_not_APPROVED() {
        SalesOrder order = requestedOrder();

        assertThatThrownBy(order::compensateApprove)
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void compensateDeliver_reverts_to_APPROVED_and_saga_FAILED() {
        SalesOrder order = requestedOrder();
        order.approve();
        order.markSagaProcessing();
        order.markSagaDone();
        order.deliver();
        order.markSagaProcessing();

        order.compensateDeliver();

        assertThat(order.getStatus()).isEqualTo(SalesOrderStatus.APPROVED);
        assertThat(order.getSagaStatus()).isEqualTo(SagaStatus.FAILED);
    }

    @Test
    void compensateDeliver_fails_when_not_DELIVERED() {
        SalesOrder order = requestedOrder();
        order.approve();

        assertThatThrownBy(order::compensateDeliver)
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void reverted_REQUESTED_can_be_approved_again() {
        SalesOrder order = requestedOrder();
        order.approve();
        order.markSagaProcessing();
        order.compensateApprove();

        order.approve();

        assertThat(order.getStatus()).isEqualTo(SalesOrderStatus.APPROVED);
        assertThat(order.getSagaStatus()).isEqualTo(SagaStatus.SENDING);
    }
}
