package com.fallguys.salesservice.application.port.outbound;

public interface LoadBranchSalesOrdersPort {
    SalesOrderSummaryPage load(BranchSalesOrderFilter filter);
}
