package com.fallguys.salesservice.application.port.inbound;

import com.fallguys.salesservice.application.port.outbound.HqSalesOrderKpi;
import com.fallguys.salesservice.domain.model.UserRole;

public interface GetHqSalesOrderKpiUseCase {
    HqSalesOrderKpi getKpi(UserRole role);
}
