package com.fallguys.salesservice.application.port.inbound;

import com.fallguys.salesservice.domain.model.Priority;

public record CreateSalesOrderLineCommand(
        String itemCode,
        int quantity,
        Priority priority
) {
}
