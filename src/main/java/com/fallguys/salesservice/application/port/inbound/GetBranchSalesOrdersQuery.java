package com.fallguys.salesservice.application.port.inbound;

import com.fallguys.salesservice.domain.model.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.UserRole;

import java.time.LocalDate;
import java.util.List;

public record GetBranchSalesOrdersQuery(
        String userCode,
        String warehouseCode,
        UserRole role,
        String search,
        List<SalesOrderStatus> statuses,
        LocalDate startDate,
        LocalDate endDate,
        SalesOrderSortField sortField,
        SortDirection sortDirection,
        int page,
        int size
) {}
