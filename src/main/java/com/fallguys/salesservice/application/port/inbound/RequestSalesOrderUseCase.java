package com.fallguys.salesservice.application.port.inbound;

import com.fallguys.salesservice.domain.model.SalesOrder;

public interface RequestSalesOrderUseCase {
    SalesOrder request(RequestSalesOrderCommand command);
}
