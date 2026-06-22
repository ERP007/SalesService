package com.fallguys.salesservice.adapter.outbound.messaging.event;

import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;

import java.util.List;

/**
 * 입고 요청(DELIVERED) 이벤트 payload. 발주 지점 창고(fromWarehouseCode)에 라인만큼 입고를 지시한다.
 */
public record StockInboundRequestedPayload(
        String sourceRef,
        String warehouseCode,
        List<StockLine> lines
) {
    public static StockInboundRequestedPayload from(SalesOrder order) {
        List<StockLine> lines = order.getLines().stream()
                .map(StockLine::from)
                .toList();
        return new StockInboundRequestedPayload(order.getCode(), order.getFromWarehouseCode(), lines);
    }
}
