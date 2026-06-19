package com.fallguys.salesservice.application.port.inbound.usecase;

import com.fallguys.salesservice.application.port.inbound.command.CancelSalesOrderCommand;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;

public interface CancelSalesOrderUseCase {
    SalesOrder cancel(CancelSalesOrderCommand command);
}
