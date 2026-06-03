package com.fallguys.salesservice.application.port.outbound;

import com.fallguys.salesservice.domain.model.SalesOrder;

public interface SaveSalesOrderPort {
    SalesOrder save(SalesOrder salesOrder);
}
