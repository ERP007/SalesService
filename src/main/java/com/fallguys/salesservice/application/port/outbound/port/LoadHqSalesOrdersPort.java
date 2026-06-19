package com.fallguys.salesservice.application.port.outbound.port;

import com.fallguys.salesservice.application.port.outbound.filter.HqSalesOrderFilter;
import com.fallguys.salesservice.application.port.outbound.model.HqSalesOrderSummaryPage;

public interface LoadHqSalesOrdersPort {
    HqSalesOrderSummaryPage loadOrders(HqSalesOrderFilter filter);
}
