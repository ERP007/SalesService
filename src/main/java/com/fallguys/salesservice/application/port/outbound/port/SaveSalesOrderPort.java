package com.fallguys.salesservice.application.port.outbound.port;

import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;

public interface SaveSalesOrderPort {
    SalesOrder save(SalesOrder salesOrder);
}
