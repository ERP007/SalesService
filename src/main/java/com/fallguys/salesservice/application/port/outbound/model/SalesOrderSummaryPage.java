package com.fallguys.salesservice.application.port.outbound.model;

import com.fallguys.salesservice.domain.model.salesorder.SalesOrderSummary;

import java.util.List;

public record SalesOrderSummaryPage(
        List<SalesOrderSummary> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
