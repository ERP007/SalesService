package com.fallguys.salesservice.application.port.inbound;

import com.fallguys.salesservice.domain.model.UserRole;

import java.time.LocalDate;
import java.util.List;

public record SubmitSalesOrderCommand(
        String soCode,
        String requestedBy,
        UserRole role,
        String toWarehouseCode,
        LocalDate desiredArrivalDate,
        String requestMemo,
        List<CreateSalesOrderLineCommand> lines
) {
}
