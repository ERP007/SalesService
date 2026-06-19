package com.fallguys.salesservice.adapter.outbound.persistence.salesorder;

import com.fallguys.salesservice.domain.model.salesorder.SalesOrderCancellation;
import jakarta.persistence.Embeddable;

import java.time.Instant;

@Embeddable
public record CancellationEmbeddable(
        String canceledBy,
        Instant canceledAt,
        String cancelReason
) {
    public static CancellationEmbeddable from(SalesOrderCancellation domain) {
        if (domain == null) return null;
        return new CancellationEmbeddable(domain.canceledBy(), domain.canceledAt(), domain.cancelReason());
    }

    public SalesOrderCancellation toDomain() {
        if (canceledBy == null) return null;
        return new SalesOrderCancellation(canceledBy, canceledAt, cancelReason);
    }
}
