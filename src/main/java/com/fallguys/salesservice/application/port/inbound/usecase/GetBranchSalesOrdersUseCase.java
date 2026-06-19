package com.fallguys.salesservice.application.port.inbound.usecase;

import com.fallguys.salesservice.application.port.inbound.query.GetBranchSalesOrdersQuery;
import com.fallguys.salesservice.application.port.outbound.model.SalesOrderSummaryPage;

public interface GetBranchSalesOrdersUseCase {
    SalesOrderSummaryPage getBranchOrders(GetBranchSalesOrdersQuery query);
}
