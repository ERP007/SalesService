package com.fallguys.salesservice.application.port.inbound;

import com.fallguys.salesservice.domain.model.UserRole;

public record RequestSalesOrderCommand(
        String soCode,
        String requestedBy,
        UserRole role,
        String requesterWarehouseCode
) {
}
