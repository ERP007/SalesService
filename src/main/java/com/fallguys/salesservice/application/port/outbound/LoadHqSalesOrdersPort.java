package com.fallguys.salesservice.application.port.outbound;

public interface LoadHqSalesOrdersPort {
    HqSalesOrderSummaryPage loadOrders(HqSalesOrderFilter filter);
}
