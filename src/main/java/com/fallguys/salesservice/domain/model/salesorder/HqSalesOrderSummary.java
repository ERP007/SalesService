package com.fallguys.salesservice.domain.model.salesorder;

import java.time.Instant;

public record HqSalesOrderSummary(
        String code,
        String fromWarehouseCode,
        String requestedBy,
        String requesterName,
        String requesterPosition,
        SalesOrderStatus status,
        Instant requestedAt,
        int itemCount,
        int totalQuantity,
        String unitSnapshot
) {}
