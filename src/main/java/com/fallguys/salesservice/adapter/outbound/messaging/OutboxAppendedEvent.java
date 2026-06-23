package com.fallguys.salesservice.adapter.outbound.messaging;

import java.util.UUID;

/**
 * outbox 행이 비즈니스 트랜잭션에 적재되었음을 알리는 애플리케이션 이벤트.
 * relay가 AFTER_COMMIT 단계에서 받아 해당 행을 즉시 발행한다(폴러는 누락분 보조).
 */
public record OutboxAppendedEvent(UUID eventId) {
}
