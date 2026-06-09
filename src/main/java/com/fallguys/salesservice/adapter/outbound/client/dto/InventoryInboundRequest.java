package com.fallguys.salesservice.adapter.outbound.client.dto;

import java.util.List;

public record InventoryInboundRequest(
        String sourceType,
        String sourceRef,
        String warehouseCode,
        List<InventoryInboundLineRequest> lines
) {
}
