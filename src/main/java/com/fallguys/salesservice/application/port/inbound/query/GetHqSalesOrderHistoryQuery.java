package com.fallguys.salesservice.application.port.inbound.query;

import com.fallguys.salesservice.domain.model.UserRole;

public record GetHqSalesOrderHistoryQuery(
        String soCode,
        UserRole role
) {}
