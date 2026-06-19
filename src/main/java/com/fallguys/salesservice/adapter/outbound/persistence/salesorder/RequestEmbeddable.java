package com.fallguys.salesservice.adapter.outbound.persistence.salesorder;

import com.fallguys.salesservice.domain.model.salesorder.SalesOrderRequest;
import jakarta.persistence.Embeddable;

import java.time.Instant;

@Embeddable
public record RequestEmbeddable(
        String requestedBy,
        Instant requestedAt
) {
    public static RequestEmbeddable from(SalesOrderRequest domain) {
        if (domain == null) return null;
        return new RequestEmbeddable(domain.requestedBy(), domain.requestedAt());
    }

    public SalesOrderRequest toDomain() {
        if (requestedBy == null) return null;
        return new SalesOrderRequest(requestedBy, requestedAt);
    }
}
