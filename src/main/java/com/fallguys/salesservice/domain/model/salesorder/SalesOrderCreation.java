package com.fallguys.salesservice.domain.model.salesorder;

import java.time.Instant;

public record SalesOrderCreation(
        String createdBy,
        Instant createdAt
) {
}
