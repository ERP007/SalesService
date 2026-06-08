package com.fallguys.salesservice.domain.model;

import java.time.Instant;
import java.time.LocalDate;

public record SalesOrderApproval(
        String approvedBy,
        Instant approvedAt,
        LocalDate approvedDate,
        String carrierType,
        String invoiceNumber
) {
}
