package com.fallguys.salesservice.application.port.inbound.model;

import com.fallguys.salesservice.domain.model.ActorRef;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;

import java.time.Instant;

public record SalesOrderHistoryEntry(
        SalesOrderStatus status,
        ActorRef changedBy,
        Instant changedAt
) {}
