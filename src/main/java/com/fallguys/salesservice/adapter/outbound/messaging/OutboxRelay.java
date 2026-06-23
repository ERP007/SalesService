package com.fallguys.salesservice.adapter.outbound.messaging;

import com.fallguys.salesservice.adapter.outbound.persistence.outbox.OutboxEntity;
import com.fallguys.salesservice.adapter.outbound.persistence.outbox.OutboxJpaDao;
import com.fallguys.salesservice.adapter.outbound.persistence.outbox.OutboxStatus;
import com.fallguys.salesservice.application.port.inbound.usecase.ConfirmStockEventPublishedUseCase;
import com.fallguys.salesservice.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 트랜잭셔널 outbox 릴레이. 두 경로로 발행한다.
 * 1) AFTER_COMMIT: 비즈니스 트랜잭션 커밋 직후 해당 행을 즉시 발행(주 경로).
 * 2) @Scheduled 폴러: AFTER_COMMIT 누락·발행 실패로 PENDING에 남은 행을 보조 발행.
 *
 * 발행 확정(publisher confirm ack) 시 outbox를 PUBLISHED로,
 * 발주 saga를 PROCESSING으로 전진시킨다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private static final int MAX_RETRY = 5;
    private static final long CONFIRM_TIMEOUT_SECONDS = 5;

    private final RabbitTemplate rabbitTemplate;
    private final OutboxJpaDao outboxJpaDao;
    private final ConfirmStockEventPublishedUseCase confirmStockEventPublishedUseCase;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAppended(OutboxAppendedEvent event) {
        outboxJpaDao.findById(event.eventId())
                .filter(o -> o.getStatus() == OutboxStatus.PENDING)
                .ifPresent(this::publish);
    }

    @Scheduled(fixedDelayString = "${outbox.relay.poll-delay-ms:5000}")
    public void publishPending() {
        List<OutboxEntity> pending = outboxJpaDao.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        for (OutboxEntity entity : pending) {
            publish(entity);
        }
    }

    /**
     * 한 행을 발행하고 publisher confirm을 기다린다.
     * ack → outbox PUBLISHED + saga PROCESSING. nack/timeout → retry 증가, 한계 초과 시 FAILED.
     */
    private void publish(OutboxEntity entity) {
        UUID eventId = entity.getEventId();
        Message message = MessageBuilder
                .withBody(entity.getPayload().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding(StandardCharsets.UTF_8.name())
                .setMessageId(eventId.toString())
                .build();
        CorrelationData correlation = new CorrelationData(eventId.toString());

        try {
            rabbitTemplate.send(RabbitConfig.COMMANDS_EXCHANGE, entity.getEventType().getWire(),
                    message, correlation);
            CorrelationData.Confirm confirm =
                    correlation.getFuture().get(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (confirm.ack()) {
                // saga 전진을 먼저 확정한다. outbox를 PUBLISHED로 바꾸기 전에 실패하면
                // 행이 PENDING으로 남아 폴러가 재발행→멱등 수렴한다(불일치가 self-healing).
                confirmStockEventPublishedUseCase.confirmPublished(entity.getAggregateId());
                entity.markPublished(Instant.now());
                outboxJpaDao.save(entity);
            } else {
                handleFailure(entity, "broker nack: " + confirm.reason());
            }
        } catch (Exception e) {
            handleFailure(entity, e.getMessage());
        }
    }

    private void handleFailure(OutboxEntity entity, String reason) {
        entity.increaseRetry();
        if (entity.getRetryCount() >= MAX_RETRY) {
            entity.markFailed();
            log.error("outbox 발행 실패 한계 초과 eventId={} reason={}", entity.getEventId(), reason);
        } else {
            log.warn("outbox 발행 실패 재시도 대기 eventId={} retry={} reason={}",
                    entity.getEventId(), entity.getRetryCount(), reason);
        }
        outboxJpaDao.save(entity);
    }
}
