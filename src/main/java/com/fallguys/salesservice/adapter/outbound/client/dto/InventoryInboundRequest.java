package com.fallguys.salesservice.adapter.outbound.client.dto;

import com.fallguys.salesservice.domain.model.SalesOrder;
import com.fallguys.salesservice.domain.model.SalesOrderLine;

import java.util.List;

public record InventoryInboundRequest(
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
            return new LineRequest(line.getItemCode(), line.getApprovedQuantity(), line.getId());
        }
    }

    public static InventoryInboundRequest from(SalesOrder order) {
        List<LineRequest> lines = order.getLines().stream()
                .map(LineRequest::from)
                .toList();
        return new InventoryInboundRequest(order.getCode(), order.getFromWarehouseCode(), lines);
    }
}
