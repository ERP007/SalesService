package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.domain.model.SalesOrder;
import com.fallguys.salesservice.domain.model.SalesOrderLine;
import com.fallguys.salesservice.domain.model.SalesOrderStatus;

import java.time.Instant;
import java.time.LocalDate;

public record CreateSalesOrderResponse(
        String code,
        String fromWarehouseCode,
        String toWarehouseCode,
        LocalDate desiredArrivalDate,
        SalesOrderStatus status,
        int totalQuantity,
        Instant createdAt
) {
    public static CreateSalesOrderResponse from(SalesOrder domain) {
        int totalQuantity = domain.getLines().stream()
                .mapToInt(SalesOrderLine::getRequestedQuantity)
                .sum();
        return new CreateSalesOrderResponse(
                domain.getCode(),
                domain.getFromWarehouseCode(),
                domain.getToWarehouseCode(),
                domain.getDesiredArrivalDate(),
                domain.getStatus(),
                totalQuantity,
                domain.getCreation().createdAt()
        );
    }
}
