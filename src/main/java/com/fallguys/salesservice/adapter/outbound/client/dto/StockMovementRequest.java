package com.fallguys.salesservice.adapter.outbound.client.dto;

import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;

import java.util.List;

/**
 * 재고 서비스 동기 입고/출고 요청 본문. sourceRef=발주번호, warehouseCode=대상 창고,
 * lines=품목별 수량. async 이벤트 payload와 달리 executor는 본문에 포함하지 않는다.
 */
public record StockMovementRequest(
        String sourceRef,
        String warehouseCode,
        List<Line> lines
) {
    public record Line(String sku, int quantity, Long sourceLineNo) {}

    // 출고: 본사 출고 창고(toWarehouseCode)로 라인만큼 출고 지시.
    public static StockMovementRequest forOutbound(SalesOrder order) {
        return new StockMovementRequest(order.getCode(), order.getTo().code(), lines(order));
    }

    // 입고: 발주 지점 창고(fromWarehouseCode)로 라인만큼 입고 지시.
    public static StockMovementRequest forInbound(SalesOrder order) {
        return new StockMovementRequest(order.getCode(), order.getFrom().code(), lines(order));
    }

    private static List<Line> lines(SalesOrder order) {
        return order.getLines().stream()
                .map(l -> new Line(l.getItemCode(), l.getQuantity(), l.getId()))
                .toList();
    }
}
