package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.application.port.inbound.model.SalesOrderDetail;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderRequest;
import com.fallguys.salesservice.domain.model.salesorderline.SalesOrderLine;

import java.time.Instant;
import java.util.List;

public record SalesOrderDetailResponse(
        String code,
        String status,
        String progress,
        WarehouseInfo fromWarehouse,
        WarehouseInfo toWarehouse,
        PersonInfo requester,
        Instant requestedAt,
        String requestMemo,
        PersonInfo approval,
        List<LineResponse> lines
) {
    public record LineResponse(
            Long id,
            String itemCode,
            String itemName,
            String unit,
            int requestQuantity
    ) {
        public static LineResponse from(SalesOrderLine line) {
            return new LineResponse(
                    line.getId(),
                    line.getItemCode(),
                    line.getItemNameSnapshot(),
                    line.getUnitSnapshot(),
                    line.getQuantity()
            );
        }
    }

    public static SalesOrderDetailResponse from(SalesOrderDetail detail) {
        SalesOrder order = detail.salesOrder();
        SalesOrderRequest request = order.getRequest();

        List<LineResponse> lines = detail.lines() != null
                ? detail.lines().stream().map(LineResponse::from).toList()
                : List.of();

        return new SalesOrderDetailResponse(
                order.getCode(),
                order.getStatus().name(),
                order.progress().name(),
                new WarehouseInfo(order.getFrom().code(), detail.fromWarehouseName()),
                new WarehouseInfo(order.getTo().code(), detail.toWarehouseName()),
                PersonInfo.from(detail.requester()),
                request != null ? request.requestedAt() : null,
                order.getRequestMemo(),
                PersonInfo.from(detail.approver()),
                lines
        );
    }
}
