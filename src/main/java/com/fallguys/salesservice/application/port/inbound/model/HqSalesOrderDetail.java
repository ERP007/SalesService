package com.fallguys.salesservice.application.port.inbound.model;

import com.fallguys.salesservice.domain.model.ActorRef;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;

public record HqSalesOrderDetail(
        SalesOrder salesOrder,
        String fromWarehouseName,
        String toWarehouseName,
        ActorRef requester,
        ActorRef approver
) {}
