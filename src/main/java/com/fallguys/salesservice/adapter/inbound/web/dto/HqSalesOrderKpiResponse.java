package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.application.port.outbound.model.HqSalesOrderKpi;

public record HqSalesOrderKpiResponse(
        long totalCount,
        long requestedCount,
        long approvedCount
) {
    public static HqSalesOrderKpiResponse from(HqSalesOrderKpi kpi) {
        return new HqSalesOrderKpiResponse(
                kpi.totalCount(),
                kpi.requestedCount(),
                kpi.approvedCount()
        );
    }
}
