package com.fallguys.salesservice.application.port.inbound.usecase;

import com.fallguys.salesservice.application.port.inbound.model.SalesOrderProgressView;
import com.fallguys.salesservice.application.port.inbound.query.GetSalesOrderProgressQuery;

public interface GetSalesOrderProgressUseCase {
    SalesOrderProgressView get(GetSalesOrderProgressQuery query);
}
