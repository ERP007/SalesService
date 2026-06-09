package com.fallguys.salesservice.application.port.inbound;

import com.fallguys.salesservice.domain.model.SalesOrder;

public interface RejectSalesOrderUseCase {
    SalesOrder reject(RejectSalesOrderCommand command);
}
