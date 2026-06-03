package com.fallguys.salesservice.adapter.outbound.client.dto;

import java.util.List;

public record ItemBatchResponse(
        List<ItemData> items,
        List<Long> missingIds
) {
    public record ItemData(
            String itemCode,
            String name,
            UnitData unit
    ) {
    }

    public record UnitData(
            String name
    ) {
    }
}
