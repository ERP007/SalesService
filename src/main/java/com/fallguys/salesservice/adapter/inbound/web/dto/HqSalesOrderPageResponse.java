package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.application.port.outbound.model.HqSalesOrderSummaryPage;

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
    public static HqSalesOrderPageResponse from(HqSalesOrderSummaryPage summaryPage, List<HqSalesOrderSummaryResponse> content) {
        return new HqSalesOrderPageResponse(
                content,
                summaryPage.page(),
                summaryPage.size(),
                summaryPage.totalElements(),
                summaryPage.totalPages(),
                summaryPage.hasPrevious(),
                summaryPage.hasNext()
        );
    }
}
