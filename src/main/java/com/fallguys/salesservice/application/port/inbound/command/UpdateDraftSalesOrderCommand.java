package com.fallguys.salesservice.application.port.inbound.command;

import com.fallguys.salesservice.domain.model.UserRole;

import java.util.List;

public record UpdateDraftSalesOrderCommand(
        String soCode,
        String requestedBy,
        UserRole role,
        String requesterWarehouseCode,
        String toWarehouseCode,
        String requestMemo,
        List<CreateSalesOrderLineCommand> lines
) {
}
