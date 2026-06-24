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
        return new SalesOrderRequest(toActor(), requestedAt);
    }

    /** 요청자 스냅샷을 ActorRef로 묶는다. 요청 전(DRAFT)은 null. */
    public ActorRef toActor() {
        if (requestedBy == null) return null;
        return ActorRef.of(requestedBy, requestedByName, requestedByPosition);
    }
}
