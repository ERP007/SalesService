package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.application.port.outbound.model.SalesOrderSummaryPage;
import com.fallguys.salesservice.domain.model.salesorder.HqSalesOrderSummary;

import java.util.List;

public record HqSalesOrderPageResponse(
        List<HqSalesOrderSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasPrevious,
        boolean hasNext
) {
    public static HqSalesOrderPageResponse from(SalesOrderSummaryPage<HqSalesOrderSummary> summaryPage,
                                                List<HqSalesOrderSummaryResponse> content) {
        return new HqSalesOrderPageResponse(
                content,
                summaryPage.page(),
                summaryPage.size(),
                summaryPage.totalElements(),
                summaryPage.totalPages(),
                summaryPage.page() > 1,
                summaryPage.page() < summaryPage.totalPages()
        );
    }
}
