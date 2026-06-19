package com.fallguys.salesservice.application.port.inbound.model;

import com.fallguys.salesservice.application.port.outbound.model.UserInfo;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;

import java.time.Instant;

public record SalesOrderHistoryEntry(
        SalesOrderStatus status,
        UserInfo changedBy,
        Instant changedAt
) {}
