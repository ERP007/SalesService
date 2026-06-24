package com.fallguys.salesservice.domain.model.salesorder;

import com.fallguys.salesservice.domain.model.ActorRef;

import java.time.Instant;

public record SalesOrderCreation(
        ActorRef createdBy,
        Instant createdAt
) {
}
