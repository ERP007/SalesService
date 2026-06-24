package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.domain.model.salesorder.HqSalesOrderSummary;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;

public record HqSalesOrderSummaryResponse(
        String code,
        WarehouseInfo fromWarehouse,
        RequestInfo request,
        int itemCount,
        SalesOrderStatus status
) {
    public static HqSalesOrderSummaryResponse from(HqSalesOrderSummary summary) {
        return new HqSalesOrderSummaryResponse(
                summary.code(),
                WarehouseInfo.from(summary.from()),
                RequestInfo.from(summary.request()),
                summary.itemCount(),
                summary.status()
        );
    }
}
