package com.fallguys.salesservice.application.port.outbound;

public record HqSalesOrderKpi(
        long totalCount,
        long requestedCount,
        long approvedCount,
        long delayedCount
) {
}
