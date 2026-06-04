package com.fallguys.salesservice.application.port.inbound;

import com.fallguys.salesservice.application.port.outbound.SalesOrderSummaryPage;

public interface GetBranchSalesOrdersUseCase {
    SalesOrderSummaryPage getBranchOrders(GetBranchSalesOrdersQuery query);
}
