package com.fallguys.salesservice.application.port.inbound.query;

import com.fallguys.salesservice.application.port.inbound.model.SalesOrderSortField;
import com.fallguys.salesservice.application.port.inbound.model.SortDirection;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
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
