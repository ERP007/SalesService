package com.fallguys.salesservice.application.port.outbound.port;

import com.fallguys.salesservice.application.port.outbound.filter.BranchSalesOrderFilter;
import com.fallguys.salesservice.application.port.outbound.model.SalesOrderSummaryPage;
import com.fallguys.salesservice.domain.model.salesorder.BranchSalesOrderSummary;

public interface LoadBranchSalesOrdersPort {
    SalesOrderSummaryPage<BranchSalesOrderSummary> load(BranchSalesOrderFilter filter);
}
