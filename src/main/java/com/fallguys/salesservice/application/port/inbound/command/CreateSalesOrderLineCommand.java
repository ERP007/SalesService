package com.fallguys.salesservice.application.port.inbound.command;

import com.fallguys.salesservice.domain.model.salesorderline.Priority;

public record CreateSalesOrderLineCommand(
        String itemCode,
        int quantity,
        Priority priority
) {
}
