package com.fallguys.salesservice.application.port.inbound.query;

import com.fallguys.salesservice.domain.model.UserRole;

/** warehouseCode는 지점 소유 검증용(본사는 null). */
public record GetSalesOrderProgressQuery(
        String soCode,
        UserRole role,
        String warehouseCode
) {}
