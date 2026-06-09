package com.fallguys.salesservice.application.port.inbound;

import com.fallguys.salesservice.application.port.outbound.UserInfo;
import com.fallguys.salesservice.domain.model.SalesOrderStatus;

import java.time.Instant;

public record SalesOrderHistoryEntry(
        SalesOrderStatus status,
        UserInfo changedBy,
        Instant changedAt
) {}
