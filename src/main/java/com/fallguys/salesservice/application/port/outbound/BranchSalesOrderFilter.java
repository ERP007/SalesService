package com.fallguys.salesservice.application.port.outbound;

import com.fallguys.salesservice.domain.model.SalesOrderStatus;

import java.time.Instant;
import java.util.List;

public record BranchSalesOrderFilter(
        String warehouseCode,
        String search,
        List<SalesOrderStatus> statuses,
        Instant startInstant,
        Instant endInstant,
        String sortField,
        String sortDirection,
        int page,
        int size
) {}
