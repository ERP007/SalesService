package com.fallguys.salesservice.domain.model.salesorder;

import com.fallguys.salesservice.domain.model.salesorderhistory.CarrierType;

import java.time.Instant;
import java.time.LocalDate;

public record SalesOrderApproval(
        String approvedBy,
        Instant approvedAt,
        LocalDate approvedDate,
        CarrierType carrierType,
        String invoiceNumber
) {
}
