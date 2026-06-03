package com.fallguys.salesservice.domain.model;

import java.time.Instant;

public record SalesOrderCreation(
        String createdBy,
        Instant createdAt
) {
}
