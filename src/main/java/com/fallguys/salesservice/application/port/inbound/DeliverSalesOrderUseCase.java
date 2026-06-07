package com.fallguys.salesservice.application.port.inbound;

import com.fallguys.salesservice.domain.model.SalesOrder;

public interface DeliverSalesOrderUseCase {
    SalesOrder deliver(DeliverSalesOrderCommand command);
}
