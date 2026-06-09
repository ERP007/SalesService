package com.fallguys.salesservice.adapter.outbound.client.dto;

import java.util.List;

public record ItemBatchResponse(
        List<ItemData> items,
        List<String> notFoundSkus
) {
    public record ItemData(
            String sku,
            String name,
            String unit,
            boolean active
    ) {
    }
}
