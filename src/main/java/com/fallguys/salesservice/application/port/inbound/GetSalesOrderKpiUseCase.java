package com.fallguys.salesservice.application.port.inbound;

import com.fallguys.salesservice.application.port.outbound.SalesOrderKpi;

public interface GetSalesOrderKpiUseCase {
    SalesOrderKpi getKpi(String requestedBy);
}
