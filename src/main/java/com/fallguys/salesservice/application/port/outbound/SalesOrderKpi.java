package com.fallguys.salesservice.application.port.outbound;

public record SalesOrderKpi(
        long totalCount,
        long requestedCount,
        long approvedCount,
        long deliveredCount
) {
}
