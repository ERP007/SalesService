package com.fallguys.salesservice.adapter.outbound.messaging.event;

import com.fallguys.salesservice.application.port.outbound.model.Executor;

/**
 * 재고 이벤트 payload의 수행자. code + name.
 */
public record StockExecutor(
        String empNo,
        String name
) {
    public static StockExecutor from(Executor executor) {
        return new StockExecutor(executor.code(), executor.name());
    }
}
