package com.fallguys.salesservice.application.port.inbound.usecase;

import com.fallguys.salesservice.application.port.inbound.query.GetHqSalesOrderDetailQuery;
import com.fallguys.salesservice.application.port.inbound.model.HqSalesOrderDetail;

public interface GetHqSalesOrderDetailUseCase {
    HqSalesOrderDetail get(GetHqSalesOrderDetailQuery query);
}
