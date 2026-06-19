package com.fallguys.salesservice.application.port.outbound.port;

import com.fallguys.salesservice.application.port.outbound.filter.BranchSalesOrderFilter;
import com.fallguys.salesservice.application.port.outbound.model.SalesOrderSummaryPage;

public interface LoadBranchSalesOrdersPort {
    SalesOrderSummaryPage load(BranchSalesOrderFilter filter);
}
