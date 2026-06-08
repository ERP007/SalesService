package com.fallguys.salesservice.application.port.inbound;

import com.fallguys.salesservice.application.port.outbound.HqSalesOrderSummaryPage;

public interface GetHqSalesOrdersUseCase {
    HqSalesOrderSummaryPage getOrders(GetHqSalesOrdersQuery query);
}
