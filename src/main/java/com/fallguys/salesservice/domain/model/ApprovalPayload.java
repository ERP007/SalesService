package com.fallguys.salesservice.domain.model;

import java.time.LocalDate;

public record ApprovalPayload(
        LocalDate approvedDate,
        CarrierType carrierType,
        String invoiceNumber
) implements StatusChangePayload {
}
