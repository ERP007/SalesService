package com.fallguys.salesservice.application.port.outbound;

public record BranchSalesOrderKpi(
        long totalCount,
        long draftCount,
        long requestedCount,
        long approvedCount
) {
}
