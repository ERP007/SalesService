package com.fallguys.salesservice.adapter.outbound.client.dto;

import java.util.List;

public record ItemBatchResponse(
        List<ItemData> items,
        List<String> missingCodes
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
