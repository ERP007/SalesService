package com.fallguys.salesservice.application.port.inbound;

import com.fallguys.salesservice.application.port.outbound.SalesOrderKpi;
import com.fallguys.salesservice.domain.model.UserRole;

public interface GetSalesOrderKpiUseCase {
    SalesOrderKpi getKpi(String warehouseCode, UserRole role);
}
