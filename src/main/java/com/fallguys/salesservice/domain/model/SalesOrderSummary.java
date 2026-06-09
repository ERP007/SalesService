package com.fallguys.salesservice.domain.model;

import java.time.Instant;
import java.time.LocalDate;

public record SalesOrderSummary(
        String code,
        SalesOrderStatus status,
        LocalDate desiredArrivalDate,
        Instant requestedAt,
        int itemCount,
        int totalQuantity,
        String unitSnapshot
) {}
