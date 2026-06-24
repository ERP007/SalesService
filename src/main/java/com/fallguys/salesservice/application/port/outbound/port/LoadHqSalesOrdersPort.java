package com.fallguys.salesservice.application.port.outbound.port;

import com.fallguys.salesservice.application.port.outbound.filter.HqSalesOrderFilter;
import com.fallguys.salesservice.application.port.outbound.model.SalesOrderSummaryPage;
import com.fallguys.salesservice.domain.model.salesorder.HqSalesOrderSummary;

public interface LoadHqSalesOrdersPort {
    SalesOrderSummaryPage<HqSalesOrderSummary> loadOrders(HqSalesOrderFilter filter);
}
