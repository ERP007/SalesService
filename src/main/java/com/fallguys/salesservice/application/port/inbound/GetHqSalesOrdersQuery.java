package com.fallguys.salesservice.application.port.inbound;

import com.fallguys.salesservice.domain.model.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.UserRole;

import java.time.LocalDate;
import java.util.List;

public record GetHqSalesOrdersQuery(
        UserRole role,
        String warehouseCode,
        String search,
        List<SalesOrderStatus> statuses,
        LocalDate startDate,
        LocalDate endDate,
        String sortField,
        String sortDirection,
        int page,
        int size
) {}
