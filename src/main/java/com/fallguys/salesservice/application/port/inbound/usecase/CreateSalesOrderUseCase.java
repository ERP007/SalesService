package com.fallguys.salesservice.application.port.inbound.usecase;

import com.fallguys.salesservice.application.port.inbound.command.CreateSalesOrderCommand;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;

public interface CreateSalesOrderUseCase {
    SalesOrder create(CreateSalesOrderCommand command);
}
