package com.fallguys.salesservice.application.port.inbound.command;

import com.fallguys.salesservice.domain.model.UserRole;

import java.time.LocalDate;

public record DeliverSalesOrderCommand(
        String soCode,
        String requesterWarehouseCode,
        String deliveredBy,
        String delivererName,
        UserRole role,
        LocalDate deliveredDate
) {
}
