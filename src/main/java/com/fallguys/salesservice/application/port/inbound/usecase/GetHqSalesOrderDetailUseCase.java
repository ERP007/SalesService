package com.fallguys.salesservice.application.port.inbound.usecase;

import com.fallguys.salesservice.application.port.inbound.query.GetHqSalesOrderDetailQuery;
import com.fallguys.salesservice.application.port.inbound.model.SalesOrderDetail;

public interface GetHqSalesOrderDetailUseCase {
    SalesOrderDetail get(GetHqSalesOrderDetailQuery query);
}
