package com.fallguys.salesservice.application.port.inbound;

public record CancelSalesOrderCommand(
        String soCode,
        String canceledBy,
        String reason
) {
}
