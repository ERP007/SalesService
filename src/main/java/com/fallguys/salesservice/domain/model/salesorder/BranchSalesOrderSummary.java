package com.fallguys.salesservice.domain.model.salesorder;

public record BranchSalesOrderSummary(
        String code,
        SalesOrderStatus status,
        OrderProgress progress,
        SalesOrderRequest request,
        int itemCount
) {}
