package com.fallguys.salesservice.application.port.inbound;

public interface GetBranchSalesOrderDetailUseCase {
    SalesOrderDetail get(GetBranchSalesOrderDetailQuery query);
}
