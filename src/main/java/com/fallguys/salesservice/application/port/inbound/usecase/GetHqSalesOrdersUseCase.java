package com.fallguys.salesservice.application.port.inbound.usecase;

import com.fallguys.salesservice.application.port.inbound.query.GetHqSalesOrdersQuery;
import com.fallguys.salesservice.application.port.outbound.model.HqSalesOrderSummaryPage;

public interface GetHqSalesOrdersUseCase {
    HqSalesOrderSummaryPage getOrders(GetHqSalesOrdersQuery query);
}
