package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.domain.model.SalesOrder;
import com.fallguys.salesservice.domain.model.SalesOrderLine;
import com.fallguys.salesservice.domain.model.SalesOrderStatus;

import java.time.Instant;
import java.time.LocalDate;

public record RequestSalesOrderResponse(
        String code,
        String fromWarehouseCode,
        String toWarehouseCode,
        LocalDate desiredArrivalDate,
        SalesOrderStatus status,
        int totalQuantity,
        Instant createdAt,
        Instant requestedAt
) {
    public static RequestSalesOrderResponse from(SalesOrder domain) {
        int totalQuantity = domain.getLines().stream()
                .mapToInt(SalesOrderLine::getRequestedQuantity)
                .sum();
        return new RequestSalesOrderResponse(
                domain.getCode(),
                domain.getFromWarehouseCode(),
                domain.getToWarehouseCode(),
                domain.getDesiredArrivalDate(),
                domain.getStatus(),
                totalQuantity,
                domain.getCreation().createdAt(),
                domain.getRequest().requestedAt()
        );
    }
}
