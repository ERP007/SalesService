package com.fallguys.salesservice.adapter.outbound.persistence.salesorder;

import com.fallguys.salesservice.domain.model.salesorderhistory.CarrierType;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderApproval;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.Instant;
import java.time.LocalDate;

@Embeddable
public record ApprovalEmbeddable(
        String approvedBy,
        Instant approvedAt,
        LocalDate approvedDate,
        @Enumerated(EnumType.STRING) CarrierType carrierType,
        @Column(unique = true) String invoiceNumber
) {
    public static ApprovalEmbeddable from(SalesOrderApproval domain) {
        if (domain == null) return null;
        return new ApprovalEmbeddable(domain.approvedBy(), domain.approvedAt(),
                domain.approvedDate(), domain.carrierType(), domain.invoiceNumber());
    }

    public SalesOrderApproval toDomain() {
        if (approvedBy == null) return null;
        return new SalesOrderApproval(approvedBy, approvedAt, approvedDate, carrierType, invoiceNumber);
    }
}
