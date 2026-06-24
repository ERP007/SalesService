package com.fallguys.salesservice.application.port.inbound.usecase;

import com.fallguys.salesservice.application.port.inbound.query.GetBranchSalesOrdersQuery;
import com.fallguys.salesservice.application.port.outbound.model.SalesOrderSummaryPage;
import com.fallguys.salesservice.domain.model.salesorder.BranchSalesOrderSummary;

public interface GetBranchSalesOrdersUseCase {
    SalesOrderSummaryPage<BranchSalesOrderSummary> getBranchOrders(GetBranchSalesOrdersQuery query);
}
