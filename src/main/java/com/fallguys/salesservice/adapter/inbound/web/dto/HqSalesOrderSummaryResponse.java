package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.domain.model.salesorder.HqSalesOrderSummary;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;

import java.time.Instant;

public record HqSalesOrderSummaryResponse(
        String code,
        String fromWarehouseCode,
        String requestedBy,
        String requesterName,
        String requesterPosition,
        Instant requestedAt,
        int itemCount,
        int totalQuantity,
        String unitSnapshot,
        SalesOrderStatus status
) {
    public static HqSalesOrderSummaryResponse from(HqSalesOrderSummary summary) {
        return new HqSalesOrderSummaryResponse(
                summary.code(),
                summary.fromWarehouseCode(),
                summary.requestedBy(),
                summary.requesterName(),
                summary.requesterPosition(),
                summary.requestedAt(),
                summary.itemCount(),
                summary.totalQuantity(),
                summary.unitSnapshot(),
                summary.status()
        );
    }
}
