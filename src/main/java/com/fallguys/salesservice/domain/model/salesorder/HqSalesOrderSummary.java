package com.fallguys.salesservice.domain.model.salesorder;

import com.fallguys.salesservice.domain.model.WarehouseRef;

public record HqSalesOrderSummary(
        String code,
        WarehouseRef from,
        SalesOrderStatus status,
        OrderProgress progress,
        SalesOrderRequest request,
        int itemCount
) {}
