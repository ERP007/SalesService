package com.fallguys.salesservice.adapter.outbound.persistence.salesorder;

import com.fallguys.salesservice.domain.model.salesorderhistory.RejectReasonCategory;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderRejection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.Instant;

@Embeddable
public record RejectionEmbeddable(
        String rejectedBy,
        Instant rejectedAt,
        @Enumerated(EnumType.STRING) RejectReasonCategory rejectReasonCategory,
        String rejectReasonMemo
) {
    public static RejectionEmbeddable from(SalesOrderRejection domain) {
        if (domain == null) return null;
        return new RejectionEmbeddable(domain.rejectedBy(), domain.rejectedAt(),
                domain.rejectReasonCategory(), domain.rejectReasonMemo());
    }

    public SalesOrderRejection toDomain() {
        if (rejectedBy == null) return null;
        return new SalesOrderRejection(rejectedBy, rejectedAt, rejectReasonCategory, rejectReasonMemo);
    }
}
