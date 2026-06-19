package com.fallguys.salesservice.application.port.inbound.model;

import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;

public record SalesOrderDetail(
        SalesOrder salesOrder,
        String fromWarehouseName,
        String toWarehouseName
) {
}
