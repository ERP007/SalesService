package com.fallguys.salesservice.domain.model.salesorder;

import java.time.Instant;

public record SalesOrderRequest(
        String requestedBy,
        Instant requestedAt
) {
}
