package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.application.port.outbound.BranchSalesOrderKpi;

public record BranchSalesOrderKpiResponse(
        long totalCount,
        long draftCount,
        long requestedCount,
        long approvedCount
) {
    public static BranchSalesOrderKpiResponse from(BranchSalesOrderKpi kpi) {
        return new BranchSalesOrderKpiResponse(
                kpi.totalCount(),
                kpi.draftCount(),
                kpi.requestedCount(),
                kpi.approvedCount()
        );
    }
}
