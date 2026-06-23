package com.fallguys.salesservice.application.port.inbound.usecase;

import com.fallguys.salesservice.application.port.inbound.query.GetBranchSalesOrderHistoryQuery;
import com.fallguys.salesservice.application.port.inbound.model.SalesOrderHistoryEntry;

import java.util.List;

public interface GetBranchSalesOrderHistoryUseCase {
    List<SalesOrderHistoryEntry> get(GetBranchSalesOrderHistoryQuery query);
}
