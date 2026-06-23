package com.fallguys.salesservice.adapter.outbound.messaging.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 재고 서비스로 보내는 이벤트의 wire string. broker routing key로도 사용한다.
 */
@Getter
@RequiredArgsConstructor
public enum StockEventType {
    OUTBOUND_REQUESTED("inventory.stock.outbound.requested.sales"),
    INBOUND_REQUESTED("inventory.stock.inbound.requested.sales");

    private final String wire;
}
