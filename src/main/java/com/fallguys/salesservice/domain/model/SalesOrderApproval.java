package com.fallguys.salesservice.domain.model;

import java.time.Instant;

public record SalesOrderApproval(
        String approvedBy,
        Instant approvedAt,
        String carrierType,
        String invoiceNumber
) {
}
