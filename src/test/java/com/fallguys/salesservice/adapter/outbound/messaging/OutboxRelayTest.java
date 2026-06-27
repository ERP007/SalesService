package com.fallguys.salesservice.adapter.outbound.messaging;

import com.fallguys.salesservice.adapter.outbound.messaging.event.StockEventType;
import com.fallguys.salesservice.adapter.outbound.persistence.outbox.OutboxEntity;
import com.fallguys.salesservice.adapter.outbound.persistence.outbox.OutboxJpaDao;
import com.fallguys.salesservice.adapter.outbound.persistence.outbox.OutboxStatus;
import com.fallguys.salesservice.application.port.inbound.usecase.ConfirmStockEventPublishedUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock RabbitTemplate rabbitTemplate;
    @Mock OutboxJpaDao outboxJpaDao;
    @Mock ConfirmStockEventPublishedUseCase confirmStockEventPublishedUseCase;

    @InjectMocks
    OutboxRelay relay;

    private static final String SO_CODE = "SO-2026-06-0001";

    private OutboxEntity pendingEntity() {
        return OutboxEntity.pending(UUID.randomUUID(), SO_CODE,
                StockEventType.OUTBOUND_REQUESTED, "{}", Instant.parse("2026-06-27T00:00:00Z"));
    }

    /** send 시 confirm future를 주어진 ack/reason으로 즉시 완결시킨다. */
    private void stubConfirm(boolean ack, String reason) {
        willAnswer(inv -> {
            CorrelationData correlation = inv.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(ack, reason));
            return null;
        }).given(rabbitTemplate).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
    }

    private void stubSendThrows(Throwable t) {
        willThrow(t).given(rabbitTemplate)
                .send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
    }

    @Test
    void ack면_PUBLISHED_전환하고_saga_전진() {
        OutboxEntity entity = pendingEntity();
        given(outboxJpaDao.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .willReturn(List.of(entity));
        stubConfirm(true, null);

        relay.publishPending();

        then(confirmStockEventPublishedUseCase).should().confirmPublished(SO_CODE);
        then(outboxJpaDao).should().save(entity);
        assertThat(entity.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(entity.getPublishedAt()).isNotNull();
    }

    @Test
    void 브로커_연결실패는_PENDING_유지하고_retry_안올린다() {
        OutboxEntity entity = pendingEntity();
        given(outboxJpaDao.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .willReturn(List.of(entity));
        stubSendThrows(new AmqpConnectException(new RuntimeException("connection refused")));

        relay.publishPending();

        then(outboxJpaDao).should(never()).save(any());
        then(confirmStockEventPublishedUseCase).shouldHaveNoInteractions();
        assertThat(entity.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(entity.getRetryCount()).isZero();
    }

    @Test
    void confirm_타임아웃은_PENDING_유지하고_retry_안올린다() {
        OutboxEntity entity = pendingEntity();
        given(outboxJpaDao.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .willReturn(List.of(entity));
        // future를 TimeoutException으로 완결 → get()이 ExecutionException(cause=Timeout) 던짐 → transient 판정
        willAnswer(inv -> {
            CorrelationData correlation = inv.getArgument(3);
            correlation.getFuture().completeExceptionally(new TimeoutException("confirm timeout"));
            return null;
        }).given(rabbitTemplate).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        relay.publishPending();

        then(outboxJpaDao).should(never()).save(any());
        assertThat(entity.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(entity.getRetryCount()).isZero();
    }

    @Test
    void nack는_retry_증가시키고_한계초과시_FAILED() {
        OutboxEntity entity = pendingEntity();
        for (int i = 0; i < 4; i++) {
            entity.increaseRetry();
        }
        given(outboxJpaDao.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .willReturn(List.of(entity));
        stubConfirm(false, "queue full");

        relay.publishPending();

        then(outboxJpaDao).should().save(entity);
        assertThat(entity.getRetryCount()).isEqualTo(5);
        assertThat(entity.getStatus()).isEqualTo(OutboxStatus.FAILED);
    }

    @Test
    void 영구_미상_예외는_retry_경로를_탄다() {
        OutboxEntity entity = pendingEntity();
        given(outboxJpaDao.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .willReturn(List.of(entity));
        stubSendThrows(new AmqpException("permanent failure"));

        relay.publishPending();

        then(outboxJpaDao).should().save(entity);
        assertThat(entity.getRetryCount()).isEqualTo(1);
        assertThat(entity.getStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    void 일시장애시_배치를_중단해_뒷행은_시도하지_않는다() {
        OutboxEntity first = pendingEntity();
        OutboxEntity second = pendingEntity();
        given(outboxJpaDao.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .willReturn(List.of(first, second));
        stubSendThrows(new AmqpConnectException(new RuntimeException("connection refused")));

        relay.publishPending();

        // 첫 행에서 일시 장애 → break. send는 한 번만 호출된다.
        then(rabbitTemplate).should(times(1))
                .send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
        then(outboxJpaDao).should(never()).save(any());
    }
}
