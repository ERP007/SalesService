package com.fallguys.salesservice.adapter.outbound.persistence;

import com.fallguys.salesservice.domain.model.SalesOrderCreation;
import jakarta.persistence.Embeddable;

import java.time.Instant;

@Embeddable
public record CreationEmbeddable(
        String createdBy,
        Instant createdAt
) {
    public static CreationEmbeddable from(SalesOrderCreation domain) {
        return new CreationEmbeddable(domain.createdBy(), domain.createdAt());
    }

    public SalesOrderCreation toDomain() {
        return new SalesOrderCreation(createdBy, createdAt);
    }
}
