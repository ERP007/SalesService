package com.fallguys.salesservice.application.port.inbound.usecase;

import com.fallguys.salesservice.application.port.inbound.query.GetHqSalesOrdersQuery;
import com.fallguys.salesservice.application.port.outbound.model.SalesOrderSummaryPage;
import com.fallguys.salesservice.domain.model.salesorder.HqSalesOrderSummary;

public interface GetHqSalesOrdersUseCase {
    SalesOrderSummaryPage<HqSalesOrderSummary> getOrders(GetHqSalesOrdersQuery query);
}
