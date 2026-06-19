package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;

import java.time.Instant;

public record RejectSalesOrderResponse(
        String code,
        String status,
        String reasonCategory,
        String memo,
        String rejectedBy,
        Instant rejectedAt
) {
    public static RejectSalesOrderResponse from(SalesOrder order) {
        return new RejectSalesOrderResponse(
                order.getCode(),
                order.getStatus().name(),
                order.getRejection().rejectReasonCategory().name(),
                order.getRejection().rejectReasonMemo(),
                order.getRejection().rejectedBy(),
                order.getRejection().rejectedAt()
        );
    }
}
