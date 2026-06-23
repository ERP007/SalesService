package com.fallguys.salesservice.application.port.inbound.usecase;

import com.fallguys.salesservice.application.port.inbound.command.SubmitSalesOrderCommand;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;

public interface SubmitSalesOrderUseCase {
    SalesOrder submit(SubmitSalesOrderCommand command);
}
