package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.domain.model.SalesOrder;
import com.fallguys.salesservice.domain.model.SalesOrderLine;
import com.fallguys.salesservice.domain.model.SalesOrderStatus;

import java.time.Instant;
import java.time.LocalDate;

public record DeliverSalesOrderResponse(
        String code,
        String fromWarehouseCode,
        String toWarehouseCode,
        LocalDate deliveredDate,
        SalesOrderStatus status,
        int totalQuantity,
        Instant deliveredAt
) {
    public static DeliverSalesOrderResponse from(SalesOrder domain) {
        int totalQuantity = domain.getLines().stream()
                .mapToInt(SalesOrderLine::getApprovedQuantity)
                .sum();
        return new DeliverSalesOrderResponse(
                domain.getCode(),
                domain.getFromWarehouseCode(),
                domain.getToWarehouseCode(),
                domain.getDelivery().deliveredDate(),
                domain.getStatus(),
                totalQuantity,
                domain.getDelivery().deliveredAt()
        );
    }
}
