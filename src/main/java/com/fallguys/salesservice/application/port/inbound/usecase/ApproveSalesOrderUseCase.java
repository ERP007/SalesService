package com.fallguys.salesservice.application.port.inbound.usecase;

import com.fallguys.salesservice.application.port.inbound.command.ApproveSalesOrderCommand;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;

public interface ApproveSalesOrderUseCase {
    SalesOrder approve(ApproveSalesOrderCommand command);
}
