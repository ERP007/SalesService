package com.fallguys.salesservice.application.port.inbound.query;

import com.fallguys.salesservice.domain.model.UserRole;

public record GetBranchSalesOrderHistoryQuery(
        String soCode,
        UserRole role,
        String warehouseCode
) {}
