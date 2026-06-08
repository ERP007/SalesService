package com.fallguys.salesservice.application.port.inbound;

import com.fallguys.salesservice.domain.model.UserRole;

import java.time.LocalDate;

public record ApproveSalesOrderCommand(
        String soCode,
        String approvedBy,
        UserRole role,
        LocalDate approvedDate,
        String carrierType,
        String invoiceNumber
) {
}
