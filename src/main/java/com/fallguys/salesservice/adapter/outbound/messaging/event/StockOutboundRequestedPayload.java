package com.fallguys.salesservice.adapter.outbound.messaging.event;

import com.fallguys.salesservice.application.port.outbound.model.Executor;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;

import java.util.List;

/**
 * 출고 요청(APPROVED) 이벤트 payload. 본사 출고 창고(toWarehouseCode)에서 라인만큼 출고를 지시한다.
 */
public record StockOutboundRequestedPayload(
        String sourceRef,
        String warehouseCode,
        StockExecutor executor,
        List<StockLine> lines
) {
    public static StockOutboundRequestedPayload from(SalesOrder order, Executor executor) {
        List<StockLine> lines = order.getLines().stream()
                .map(StockLine::from)
                .toList();
        return new StockOutboundRequestedPayload(
                order.getCode(), order.getToWarehouseCode(), StockExecutor.from(executor), lines);
    }
}
