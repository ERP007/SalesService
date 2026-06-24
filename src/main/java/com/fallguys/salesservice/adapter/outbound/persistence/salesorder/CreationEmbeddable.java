package com.fallguys.salesservice.adapter.outbound.persistence.salesorder;

import com.fallguys.salesservice.domain.model.ActorRef;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderCreation;
import jakarta.persistence.Embeddable;

import java.time.Instant;

@Embeddable
public record CreationEmbeddable(
        String createdBy,
        String createdByName,
        String createdByPosition,
        Instant createdAt
) {
    public static CreationEmbeddable from(SalesOrderCreation domain) {
        ActorRef actor = domain.createdBy();
        return new CreationEmbeddable(
                actor.code(), actor.nameSnapshot(), actor.positionSnapshot(), domain.createdAt());
    }

    public SalesOrderCreation toDomain() {
        return new SalesOrderCreation(
                ActorRef.of(createdBy, createdByName, createdByPosition), createdAt);
    }
}
