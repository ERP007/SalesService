package com.fallguys.salesservice.application.port.inbound;

import com.fallguys.salesservice.application.port.outbound.UserInfo;
import com.fallguys.salesservice.domain.model.SalesOrder;

public record HqSalesOrderDetail(
        SalesOrder salesOrder,
        String fromWarehouseName,
        String toWarehouseName,
        UserInfo requesterInfo,
        UserInfo approverInfo
) {}
