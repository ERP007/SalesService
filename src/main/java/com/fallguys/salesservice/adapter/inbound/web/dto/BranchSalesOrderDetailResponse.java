package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.application.port.inbound.model.SalesOrderDetail;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorderline.SalesOrderLine;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record BranchSalesOrderDetailResponse(
        String code,
        String status,
        WarehouseInfo fromWarehouse,
        WarehouseInfo toWarehouse,
        LocalDate desiredArrivalDate,
        String memo,
        Instant approvedAt,
        String invoiceNumber,
        String carrierType,
        List<LineResponse> lines
) {
    public record LineResponse(
            Long id,
            String itemCode,
            String itemName,
            String unit,
            int requestQuantity,
            String priority
    ) {
        public static LineResponse from(SalesOrderLine line) {
            return new LineResponse(
                    line.getId(),
                    line.getItemCode(),
                    line.getItemNameSnapshot(),
                    line.getUnitSnapshot(),
                    line.getRequestedQuantity(),
                    line.getPriority().name()
            );
        }
    }

    public static BranchSalesOrderDetailResponse from(SalesOrderDetail detail) {
        SalesOrder order = detail.salesOrder();
        Instant approvedAt = detail.approvedAt();
        String invoiceNumber = detail.invoiceNumber();
        String carrierType = detail.carrierType() != null ? detail.carrierType().name() : null;

        List<LineResponse> lines = order.getLines() != null
                ? order.getLines().stream().map(LineResponse::from).toList()
                : List.of();

        return new BranchSalesOrderDetailResponse(
                order.getCode(),
                order.getStatus().name(),
                new WarehouseInfo(order.getFromWarehouseCode(), detail.fromWarehouseName()),
                new WarehouseInfo(order.getToWarehouseCode(), detail.toWarehouseName()),
                order.getDesiredArrivalDate(),
                order.getRequestMemo(),
                approvedAt,
                invoiceNumber,
                carrierType,
                lines
        );
    }
}
