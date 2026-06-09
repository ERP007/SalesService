package com.fallguys.salesservice.application.port.inbound;

public interface GetHqSalesOrderDetailUseCase {
    HqSalesOrderDetail get(GetHqSalesOrderDetailQuery query);
}
