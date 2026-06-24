package com.fallguys.salesservice.application.port.inbound.command;

import com.fallguys.salesservice.domain.model.UserRole;

import java.util.List;

public record SubmitSalesOrderCommand(
        String soCode,
        String requestedBy,
        String requesterName,
        String requesterPosition,
        UserRole role,
        String requesterWarehouseCode,
        String toWarehouseCode,
        String requestMemo,
        List<CreateSalesOrderLineCommand> lines
) {
}
