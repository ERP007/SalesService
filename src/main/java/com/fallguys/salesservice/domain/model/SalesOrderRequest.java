package com.fallguys.salesservice.domain.model;

import java.time.Instant;

public record SalesOrderRequest(
        String requestedBy,
        Instant requestedAt
) {
}
