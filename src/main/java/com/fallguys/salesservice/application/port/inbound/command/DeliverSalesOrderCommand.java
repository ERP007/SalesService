package com.fallguys.salesservice.application.port.inbound.command;

import com.fallguys.salesservice.domain.model.UserRole;

import java.time.LocalDate;

public record DeliverSalesOrderCommand(
        String soCode,
        String requesterWarehouseCode,
        String deliveredBy,
        UserRole role,
        LocalDate deliveredDate
) {
}
