package com.fallguys.salesservice.application.port.inbound;

import com.fallguys.salesservice.domain.model.SalesOrderStatus;

import java.time.LocalDate;
import java.util.List;

public record CreateSalesOrderCommand(
        String toWarehouseCode,
        LocalDate desiredArrivalDate,
        String requestMemo,
        SalesOrderStatus status,
        List<CreateSalesOrderLineCommand> lines,
        String requestedBy
) {
}
