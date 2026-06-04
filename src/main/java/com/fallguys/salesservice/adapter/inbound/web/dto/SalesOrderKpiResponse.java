package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.application.port.outbound.SalesOrderKpi;

public record SalesOrderKpiResponse(
        long totalCount,
        long draftCount,
        long requestedCount,
        long approvedCount
) {
    public static SalesOrderKpiResponse from(SalesOrderKpi kpi) {
        return new SalesOrderKpiResponse(
                kpi.totalCount(),
                kpi.draftCount(),
                kpi.requestedCount(),
                kpi.approvedCount()
        );
    }
}
