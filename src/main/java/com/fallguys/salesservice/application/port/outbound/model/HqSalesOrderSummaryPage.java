package com.fallguys.salesservice.application.port.outbound.model;

import com.fallguys.salesservice.domain.model.salesorder.HqSalesOrderSummary;

import java.util.List;

public record HqSalesOrderSummaryPage(
        List<HqSalesOrderSummary> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasPrevious,
        boolean hasNext
) {}
