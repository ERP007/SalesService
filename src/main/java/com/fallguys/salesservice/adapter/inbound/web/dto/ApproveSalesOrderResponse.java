package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.domain.model.SalesOrder;
import com.fallguys.salesservice.domain.model.SalesOrderLine;

import java.time.Instant;
import java.time.LocalDate;

public record ApproveSalesOrderResponse(
        String code,
        String fromWarehouseCode,
        String toWarehouseCode,
        LocalDate approvedDate,
        String carrierType,
        String invoiceNumber,
        String status,
        int totalQuantity,
        Instant approvedAt
) {
    public static ApproveSalesOrderResponse from(SalesOrder order) {
        int totalQuantity = order.getLines().stream()
                .mapToInt(SalesOrderLine::getRequestedQuantity)
                .sum();
        return new ApproveSalesOrderResponse(
                order.getCode(),
                order.getFromWarehouseCode(),
                order.getToWarehouseCode(),
                order.getApproval().approvedDate(),
                order.getApproval().carrierType(),
                order.getApproval().invoiceNumber(),
                order.getStatus().name(),
                totalQuantity,
                order.getApproval().approvedAt()
        );
    }
}
