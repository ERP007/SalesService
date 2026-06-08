package com.fallguys.salesservice.application.port.inbound;

import java.util.List;

public interface GetBranchSalesOrderHistoryUseCase {
    List<SalesOrderHistoryEntry> get(GetBranchSalesOrderHistoryQuery query);
}
