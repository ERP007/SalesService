package com.fallguys.salesservice.domain.model.salesorder;

public record BranchSalesOrderSummary(
        String code,
        SalesOrderStatus status,
        SalesOrderRequest request,
        int itemCount
) {}
