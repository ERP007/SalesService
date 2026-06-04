package com.fallguys.salesservice.adapter.inbound.web.dto;

public record SalesOrderKpiResponse(
        long totalCount,
        long requestedCount,
        long approvedCount,
        long deliveredCount
) {
}
