package com.fallguys.salesservice.application.port.inbound;

import com.fallguys.salesservice.domain.model.UserRole;

public record GetHqSalesOrderDetailQuery(
        String soCode,
        UserRole role
) {}
