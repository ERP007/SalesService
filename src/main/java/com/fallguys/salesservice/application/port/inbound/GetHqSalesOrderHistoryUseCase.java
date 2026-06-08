package com.fallguys.salesservice.application.port.inbound;

import java.util.List;

public interface GetHqSalesOrderHistoryUseCase {
    List<SalesOrderHistoryEntry> get(GetHqSalesOrderHistoryQuery query);
}
