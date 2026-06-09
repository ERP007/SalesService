package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.application.port.inbound.HqSalesOrderDetail;
import com.fallguys.salesservice.domain.model.SalesOrder;
import com.fallguys.salesservice.domain.model.SalesOrderApproval;
import com.fallguys.salesservice.domain.model.SalesOrderLine;
import com.fallguys.salesservice.domain.model.SalesOrderRequest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record HqSalesOrderDetailResponse(
        String code,
        String status,
        WarehouseInfo fromWarehouse,
        WarehouseInfo toWarehouse,
        PersonInfo requester,
        Instant requestedAt,
        String requestMemo,
        LocalDate desiredArrivalDate,
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
                    line.getRequestedQuantity()
            );
        }
    }

    public static HqSalesOrderDetailResponse from(HqSalesOrderDetail detail) {
        SalesOrder order = detail.salesOrder();
        SalesOrderRequest request = order.getRequest();
        SalesOrderApproval approval = order.getApproval();

        List<LineResponse> lines = order.getLines() != null
                ? order.getLines().stream().map(LineResponse::from).toList()
                : List.of();

        return new HqSalesOrderDetailResponse(
                order.getCode(),
                order.getStatus().name(),
                new WarehouseInfo(order.getFromWarehouseCode(), detail.fromWarehouseName()),
                new WarehouseInfo(order.getToWarehouseCode(), detail.toWarehouseName()),
                PersonInfo.from(detail.requesterInfo()),
                request != null ? request.requestedAt() : null,
                order.getRequestMemo(),
                order.getDesiredArrivalDate(),
                approval != null ? PersonInfo.from(detail.approverInfo()) : null,
                lines
        );
    }
}
