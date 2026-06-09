package com.fallguys.salesservice.adapter.outbound.client.dto;

import java.util.List;

public record InventoryOutboundRequest(
        String sourceType,
        String sourceRef,
        String warehouseCode,
        List<InventoryOutboundLineRequest> lines
) {
}
