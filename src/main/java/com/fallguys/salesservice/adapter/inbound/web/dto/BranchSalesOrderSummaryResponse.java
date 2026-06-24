package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderSummary;

import java.time.Instant;

public record BranchSalesOrderSummaryResponse(
        String code,
        SalesOrderStatus status,
        Instant requestedAt,
        int itemCount,
        int totalQuantity,
        String unitSnapshot
) {
    public static BranchSalesOrderSummaryResponse from(SalesOrderSummary summary) {
        return new BranchSalesOrderSummaryResponse(
                summary.code(),
                summary.status(),
                summary.requestedAt(),
                summary.itemCount(),
                summary.totalQuantity(),
                summary.unitSnapshot()
        );
    }
}
