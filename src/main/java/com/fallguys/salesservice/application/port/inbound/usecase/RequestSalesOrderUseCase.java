package com.fallguys.salesservice.application.port.inbound.usecase;

import com.fallguys.salesservice.application.port.inbound.command.RequestSalesOrderCommand;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;

public interface RequestSalesOrderUseCase {
    SalesOrder request(RequestSalesOrderCommand command);
}
