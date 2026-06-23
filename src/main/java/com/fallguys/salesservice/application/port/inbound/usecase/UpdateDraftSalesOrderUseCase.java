package com.fallguys.salesservice.application.port.inbound.usecase;

import com.fallguys.salesservice.application.port.inbound.command.UpdateDraftSalesOrderCommand;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;

public interface UpdateDraftSalesOrderUseCase {
    SalesOrder updateDraft(UpdateDraftSalesOrderCommand command);
}
