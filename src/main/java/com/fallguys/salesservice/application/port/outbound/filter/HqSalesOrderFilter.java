package com.fallguys.salesservice.application.port.outbound.filter;

import com.fallguys.salesservice.application.port.inbound.model.SalesOrderSortField;
import com.fallguys.salesservice.application.port.inbound.model.SortDirection;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;

import java.time.Instant;
import java.util.List;

public record HqSalesOrderFilter(
        String warehouseCode,
        String search,
        List<SalesOrderStatus> statuses,
        Instant startInstant,
        Instant endInstant,
        SalesOrderSortField sortField,
        SortDirection sortDirection,
        int page,
        int size
) {}
