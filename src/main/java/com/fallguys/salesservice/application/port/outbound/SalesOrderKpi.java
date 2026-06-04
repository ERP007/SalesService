package com.fallguys.salesservice.application.port.outbound;

public record SalesOrderKpi(
        long totalCount,
        long draftCount,
        long requestedCount,
        long approvedCount
) {
}
