package com.fallguys.salesservice.adapter.outbound.persistence;

import com.fallguys.salesservice.domain.model.SalesOrderApproval;
import jakarta.persistence.Embeddable;

import java.time.Instant;

@Embeddable
public record ApprovalEmbeddable(
        String approvedBy,
        Instant approvedAt,
        String carrierType,
        String invoiceNumber
) {
    public static ApprovalEmbeddable from(SalesOrderApproval domain) {
        if (domain == null) return null;
        return new ApprovalEmbeddable(domain.approvedBy(), domain.approvedAt(),
                domain.carrierType(), domain.invoiceNumber());
    }

    public SalesOrderApproval toDomain() {
        if (approvedBy == null) return null;
        return new SalesOrderApproval(approvedBy, approvedAt, carrierType, invoiceNumber);
    }
}
