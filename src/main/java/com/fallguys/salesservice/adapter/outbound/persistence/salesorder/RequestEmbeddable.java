package com.fallguys.salesservice.adapter.outbound.persistence.salesorder;

import com.fallguys.salesservice.domain.model.ActorRef;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderRequest;
import jakarta.persistence.Embeddable;

import java.time.Instant;

@Embeddable
public record RequestEmbeddable(
        String requestedBy,
        String requestedByName,
        String requestedByPosition,
        Instant requestedAt
) {
    public static RequestEmbeddable from(SalesOrderRequest domain) {
        if (domain == null) return null;
        ActorRef actor = domain.requestedBy();
        return new RequestEmbeddable(
                actor.code(), actor.nameSnapshot(), actor.positionSnapshot(), domain.requestedAt());
    }

    public SalesOrderRequest toDomain() {
        if (requestedBy == null) return null;
        return new SalesOrderRequest(
                ActorRef.of(requestedBy, requestedByName, requestedByPosition), requestedAt);
    }
}
