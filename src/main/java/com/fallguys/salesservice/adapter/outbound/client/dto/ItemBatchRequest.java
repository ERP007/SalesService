package com.fallguys.salesservice.adapter.outbound.client.dto;

import java.util.List;

public record ItemBatchRequest(
        List<String> codes
) {
}
