package com.fallguys.salesservice.adapter.outbound.messaging;

import com.fallguys.salesservice.adapter.outbound.messaging.event.BaseEvent;
import com.fallguys.salesservice.adapter.outbound.messaging.event.StockEventType;
import com.fallguys.salesservice.adapter.outbound.messaging.event.StockInboundRequestedPayload;
import com.fallguys.salesservice.adapter.outbound.messaging.event.StockOutboundRequestedPayload;
import com.fallguys.salesservice.adapter.outbound.persistence.outbox.OutboxEntity;
import com.fallguys.salesservice.adapter.outbound.persistence.outbox.OutboxJpaDao;
import com.fallguys.salesservice.application.port.outbound.model.Executor;
import com.fallguys.salesservice.application.port.outbound.port.InboundStockPort;
import com.fallguys.salesservice.application.port.outbound.port.OutboundStockPort;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

/**
 * 재고 출고/입고를 동기 REST 대신 트랜잭셔널 outbox로 적재한다.
 * 호출자(approve/deliver 서비스)의 트랜잭션에 합류하므로 비즈니스 변경과 원자적으로 커밋된다.
 * 커밋 후 {@link OutboxRelay}가 OutboxAppendedEvent를 받아 broker로 발행한다.
 */
@Component
@RequiredArgsConstructor
public class StockEventMessagingAdapter implements OutboundStockPort, InboundStockPort {

    private final OutboxJpaDao outboxJpaDao;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void outbound(SalesOrder order, Executor executor) {
        append(StockEventType.OUTBOUND_REQUESTED, order.getCode(),
                StockOutboundRequestedPayload.from(order, executor));
    }

    @Override
    public void inbound(SalesOrder order, Executor executor) {
        append(StockEventType.INBOUND_REQUESTED, order.getCode(),
                StockInboundRequestedPayload.from(order, executor));
    }

    private void append(StockEventType type, String correlationId, Object payload) {
        Instant now = Instant.now();
        BaseEvent<Object> event = BaseEvent.of(type.getWire(), correlationId, payload, now);
        OutboxEntity entity = OutboxEntity.pending(
                event.eventId(), correlationId, type,
                objectMapper.writeValueAsString(event), now);
        outboxJpaDao.save(entity);
        eventPublisher.publishEvent(new OutboxAppendedEvent(event.eventId()));
    }
}
