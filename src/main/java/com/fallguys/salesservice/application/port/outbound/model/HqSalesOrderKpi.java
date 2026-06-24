package com.fallguys.salesservice.application.port.outbound.model;

public record HqSalesOrderKpi(
        long totalCount,
        long requestedCount,
        long approvedCount
) {
}
