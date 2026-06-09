package com.fallguys.salesservice.application.port.inbound;

import com.fallguys.salesservice.domain.model.UserRole;

public record CancelSalesOrderCommand(
        String soCode,
        String canceledBy,
        UserRole role,
        String requesterWarehouseCode,
        String reason
) {
}
