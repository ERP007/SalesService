package com.fallguys.salesservice.application.port.inbound.command;

import com.fallguys.salesservice.domain.model.UserRole;

public record RequestSalesOrderCommand(
        String soCode,
        String requestedBy,
        String requesterName,
        String requesterPosition,
        UserRole role,
        String requesterWarehouseCode
) {
}
