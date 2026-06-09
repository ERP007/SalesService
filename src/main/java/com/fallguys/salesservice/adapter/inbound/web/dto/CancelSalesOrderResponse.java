package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.domain.model.SalesOrder;

import java.time.Instant;

public record CancelSalesOrderResponse(
        String code,
        String status,
        String canceledBy,
        Instant canceledAt,
        String reason
) {
    public static CancelSalesOrderResponse from(SalesOrder order) {
        return new CancelSalesOrderResponse(
                order.getCode(),
                order.getStatus().name(),
                order.getCancellation().canceledBy(),
                order.getCancellation().canceledAt(),
                order.getCancellation().cancelReason()
        );
    }
}
