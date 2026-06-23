package com.fallguys.salesservice.application.port.inbound.command;

import com.fallguys.salesservice.domain.model.salesorderhistory.CarrierType;
import com.fallguys.salesservice.domain.model.UserRole;

import java.time.LocalDate;

public record ApproveSalesOrderCommand(
        String soCode,
        String approvedBy,
        String approverName,
        UserRole role,
        LocalDate approvedDate,
        CarrierType carrierType,
        String invoiceNumber
) {
}
