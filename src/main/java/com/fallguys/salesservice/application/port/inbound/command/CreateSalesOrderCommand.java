package com.fallguys.salesservice.application.port.inbound.command;

import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.UserRole;

import java.time.LocalDate;
import java.util.List;

public record CreateSalesOrderCommand(
        String fromWarehouseCode,
        String toWarehouseCode,
        LocalDate desiredArrivalDate,
        String requestMemo,
        SalesOrderStatus status,
        List<CreateSalesOrderLineCommand> lines,
        String requestedBy,
        UserRole role
) {
}
