package com.fallguys.salesservice.domain.model.salesorder;

import java.time.Instant;

public record SalesOrderCancellation(
        String canceledBy,
        Instant canceledAt,
        String cancelReason
) {
}
