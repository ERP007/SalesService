package com.fallguys.salesservice.domain.model;

import java.time.Instant;
import java.time.LocalDate;

public record HqSalesOrderSummary(
        String code,
        String fromWarehouseCode,
        String requestedBy,
        SalesOrderStatus status,
        Instant requestedAt,
        LocalDate desiredArrivalDate,
        int itemCount,
        int totalQuantity,
        String unitSnapshot
) {}
