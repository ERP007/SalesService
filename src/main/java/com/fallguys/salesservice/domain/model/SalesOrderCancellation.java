package com.fallguys.salesservice.domain.model;

import java.time.Instant;

public record SalesOrderCancellation(
        String canceledBy,
        Instant canceledAt,
        String cancelReason
) {}
