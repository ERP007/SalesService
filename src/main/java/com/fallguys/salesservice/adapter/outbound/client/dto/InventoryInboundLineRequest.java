package com.fallguys.salesservice.adapter.outbound.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InventoryInboundLineRequest(
        String sku,
        int quantity,
        @JsonProperty("source_line_no") Long sourceLineNo
) {
}
