package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.application.port.outbound.SalesOrderSummaryPage;

import java.util.List;

public record BranchSalesOrderPageResponse(
        List<BranchSalesOrderSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasPrevious,
        boolean hasNext
) {
    public static BranchSalesOrderPageResponse from(SalesOrderSummaryPage summaryPage) {
        List<BranchSalesOrderSummaryResponse> content = summaryPage.content().stream()
                .map(BranchSalesOrderSummaryResponse::from)
                .toList();
        return new BranchSalesOrderPageResponse(
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
