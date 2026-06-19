package com.fallguys.salesservice.application.port.inbound.query;

import com.fallguys.salesservice.domain.model.UserRole;

public record GetBranchSalesOrderDetailQuery(
        String soCode,
        String userCode,
        UserRole role,
        String warehouseCode
) {
}
