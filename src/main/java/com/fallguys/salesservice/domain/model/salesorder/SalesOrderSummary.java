package com.fallguys.salesservice.domain.model.salesorder;

import java.time.Instant;

public record SalesOrderSummary(
        String code,
        SalesOrderStatus status,
        Instant requestedAt,
        int itemCount,
        int totalQuantity,
        String unitSnapshot
) {}
