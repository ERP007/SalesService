package com.fallguys.salesservice.application.port.inbound.command;

import com.fallguys.salesservice.domain.model.UserRole;

public record CancelSalesOrderCommand(
        String soCode,
        String canceledBy,
        String canceledByName,
        String canceledByPosition,
        UserRole role,
        String requesterWarehouseCode,
        String reason
) {
}
