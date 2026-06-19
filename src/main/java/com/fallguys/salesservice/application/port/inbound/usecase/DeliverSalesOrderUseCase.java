package com.fallguys.salesservice.application.port.inbound.usecase;

import com.fallguys.salesservice.application.port.inbound.command.DeliverSalesOrderCommand;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;

public interface DeliverSalesOrderUseCase {
    SalesOrder deliver(DeliverSalesOrderCommand command);
}
