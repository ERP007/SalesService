package com.fallguys.salesservice.adapter.outbound.client.dto;

import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorderline.SalesOrderLine;

import java.util.List;

public record InventoryOutboundRequest(
        String sourceRef,
        String warehouseCode,
        List<LineRequest> lines
) {
    public record LineRequest(
            String sku,
            int quantity,
            Long sourceLineNo
    ) {
        public static LineRequest from(SalesOrderLine line) {
            return new LineRequest(line.getItemCode(), line.getQuantity(), line.getId());
        }
    }

    public static InventoryOutboundRequest from(SalesOrder order) {
        List<LineRequest> lines = order.getLines().stream()
                .map(LineRequest::from)
                .toList();
        return new InventoryOutboundRequest(order.getCode(), order.getToWarehouseCode(), lines);
    }
}
