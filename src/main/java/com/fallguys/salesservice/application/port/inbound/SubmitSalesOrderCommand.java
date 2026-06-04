package com.fallguys.salesservice.application.port.inbound;

import java.time.LocalDate;
import java.util.List;

public record SubmitSalesOrderCommand(
        String soCode,
        String requestedBy,
        String toWarehouseCode,
        LocalDate desiredArrivalDate,
        String requestMemo,
        List<CreateSalesOrderLineCommand> lines
) {
}
