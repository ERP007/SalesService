package com.fallguys.salesservice.application.port.inbound.model;

import com.fallguys.salesservice.domain.model.ActorRef;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.salesorderhistory.StatusChangePayload;

import java.time.Instant;

public record SalesOrderHistoryEntry(
        SalesOrderStatus status,
        ActorRef changedBy,
        Instant changedAt,
        StatusChangePayload payload
) {}
