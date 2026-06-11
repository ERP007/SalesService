package com.fallguys.salesservice.application.port.outbound;

import com.fallguys.salesservice.application.port.inbound.SalesOrderSortField;
import com.fallguys.salesservice.application.port.inbound.SortDirection;
import com.fallguys.salesservice.domain.model.SalesOrderStatus;

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
