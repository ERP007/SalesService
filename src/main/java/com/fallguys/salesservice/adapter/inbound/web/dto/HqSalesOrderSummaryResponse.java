package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.domain.model.HqSalesOrderSummary;
import com.fallguys.salesservice.domain.model.SalesOrderStatus;

import java.time.Instant;
import java.time.LocalDate;

public record HqSalesOrderSummaryResponse(
        String code,
        String fromWarehouseCode,
        String requestedBy,
        String requesterName,
        String requesterPosition,
        Instant requestedAt,
        LocalDate desiredArrivalDate,
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
                summary.desiredArrivalDate(),
                summary.itemCount(),
                summary.totalQuantity(),
                summary.unitSnapshot(),
                summary.status()
        );
    }
}
