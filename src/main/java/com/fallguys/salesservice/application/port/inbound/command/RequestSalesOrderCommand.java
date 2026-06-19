package com.fallguys.salesservice.application.port.inbound.command;

import com.fallguys.salesservice.domain.model.UserRole;

public record RequestSalesOrderCommand(
        String soCode,
        String requestedBy,
        UserRole role,
        String requesterWarehouseCode
) {
}
