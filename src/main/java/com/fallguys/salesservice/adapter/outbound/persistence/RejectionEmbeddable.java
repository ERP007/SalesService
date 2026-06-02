package com.fallguys.salesservice.adapter.outbound.persistence;

import com.fallguys.salesservice.domain.model.RejectReasonCategory;
import com.fallguys.salesservice.domain.model.SalesOrderRejection;
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
