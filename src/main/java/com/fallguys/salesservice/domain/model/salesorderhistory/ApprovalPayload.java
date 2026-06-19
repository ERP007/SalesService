package com.fallguys.salesservice.domain.model.salesorderhistory;

import java.time.LocalDate;

public record ApprovalPayload(
        LocalDate approvedDate,
        CarrierType carrierType,
        String invoiceNumber
) implements StatusChangePayload {
}
