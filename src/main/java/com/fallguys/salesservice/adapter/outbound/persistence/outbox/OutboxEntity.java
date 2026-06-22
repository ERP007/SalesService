package com.fallguys.salesservice.adapter.outbound.persistence.outbox;

import com.fallguys.salesservice.adapter.outbound.messaging.event.StockEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * 발행 대기 메시지. 비즈니스 변경과 같은 트랜잭션에 INSERT되어 원자성을 보장한다.
 * payload는 직렬화된 BaseEvent envelope 전체(JSON 문자열).
 */
@Entity
@Table(name = "outbox")
@Getter
@NoArgsConstructor
public class OutboxEntity {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private StockEventType eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    public static OutboxEntity pending(UUID eventId, String aggregateType, String aggregateId,
                                       StockEventType eventType, String payload, Instant now) {
        OutboxEntity entity = new OutboxEntity();
        entity.eventId = eventId;
        entity.aggregateType = aggregateType;
        entity.aggregateId = aggregateId;
        entity.eventType = eventType;
        entity.payload = payload;
        entity.status = OutboxStatus.PENDING;
        entity.retryCount = 0;
        entity.createdAt = now;
        return entity;
    }

    public void markPublished(Instant now) {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = now;
    }

    public void markFailed() {
        this.status = OutboxStatus.FAILED;
    }

    public void increaseRetry() {
        this.retryCount++;
    }
}
