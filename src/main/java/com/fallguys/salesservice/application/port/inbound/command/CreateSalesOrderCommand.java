package com.fallguys.salesservice.application.port.inbound.command;

import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.UserRole;

import java.util.List;

public record CreateSalesOrderCommand(
        String fromWarehouseCode,
        String toWarehouseCode,
        String requestMemo,
        SalesOrderStatus status,
        List<CreateSalesOrderLineCommand> lines,
        String requestedBy,
        String requesterName,
        String requesterPosition,
        UserRole role
) {
}
