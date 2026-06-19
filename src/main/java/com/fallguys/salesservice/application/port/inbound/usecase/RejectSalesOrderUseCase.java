package com.fallguys.salesservice.application.port.inbound.usecase;

import com.fallguys.salesservice.application.port.inbound.command.RejectSalesOrderCommand;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;

public interface RejectSalesOrderUseCase {
    SalesOrder reject(RejectSalesOrderCommand command);
}
