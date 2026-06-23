package com.fallguys.salesservice.adapter.outbound.messaging.event;

import java.time.Instant;
import java.util.UUID;

/**
 * 외부로 발행하는 모든 이벤트의 공통 envelope. payload(T)만 이벤트마다 다르다.
 * 직렬화된 형태 그대로 outbox에 저장되고 릴레이가 broker로 발행한다.
 *
 * - eventId        : 메시지 식별자(UUID). 소비자 멱등 처리 키.
 * - eventType      : 정해진 wire string({@link StockEventType}).
 * - eventVersion   : payload 스키마 버전.
 * - producer       : 발행 서비스 식별자(고정 "sales-service").
 * - occurredAt     : 도메인 사건 발생 시각.
 * - correlationId  : saga 추적 키(= SO code).
 */
public record BaseEvent<T>(
        UUID eventId,
        String eventType,
        int eventVersion,
        String producer,
        Instant occurredAt,
        String correlationId,
        T payload
) {
    public static final String PRODUCER = "sales-service";
    public static final int CURRENT_VERSION = 1;

    public static <T> BaseEvent<T> of(String eventType, String correlationId, T payload, Instant occurredAt) {
        return new BaseEvent<>(UUID.randomUUID(), eventType, CURRENT_VERSION, PRODUCER,
                occurredAt, correlationId, payload);
    }
}
