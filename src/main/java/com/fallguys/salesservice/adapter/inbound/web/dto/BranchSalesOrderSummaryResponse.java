package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.salesorder.BranchSalesOrderSummary;

public record BranchSalesOrderSummaryResponse(
        String code,
        SalesOrderStatus status,
        String progress,
        RequestInfo request,
        int itemCount
) {
    public static BranchSalesOrderSummaryResponse from(BranchSalesOrderSummary summary) {
        return new BranchSalesOrderSummaryResponse(
                summary.code(),
                summary.status(),
                summary.progress().name(),
                RequestInfo.from(summary.request()),
                summary.itemCount()
        );
    }
}
