package com.fallguys.salesservice.application.port.inbound.usecase;

import com.fallguys.salesservice.application.port.inbound.query.GetBranchSalesOrderDetailQuery;
import com.fallguys.salesservice.application.port.inbound.model.SalesOrderDetail;

public interface GetBranchSalesOrderDetailUseCase {
    SalesOrderDetail get(GetBranchSalesOrderDetailQuery query);
}
