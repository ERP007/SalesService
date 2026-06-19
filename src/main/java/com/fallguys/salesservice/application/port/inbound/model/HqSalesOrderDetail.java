package com.fallguys.salesservice.application.port.inbound.model;

import com.fallguys.salesservice.application.port.outbound.model.UserInfo;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;

public record HqSalesOrderDetail(
        SalesOrder salesOrder,
        String fromWarehouseName,
        String toWarehouseName,
        UserInfo requesterInfo,
        UserInfo approverInfo
) {}
