package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.application.port.outbound.SalesOrderKpi;

public record SalesOrderKpiResponse(
        long totalCount,
        long requestedCount,
        long approvedCount,
        long deliveredCount
) {
    public static SalesOrderKpiResponse from(SalesOrderKpi kpi) {
        return new SalesOrderKpiResponse(
                kpi.totalCount(),
                kpi.requestedCount(),
                kpi.approvedCount(),
                kpi.deliveredCount()
        );
    }
}
