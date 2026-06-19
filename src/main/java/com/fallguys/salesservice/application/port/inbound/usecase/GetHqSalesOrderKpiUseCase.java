package com.fallguys.salesservice.application.port.inbound.usecase;

import com.fallguys.salesservice.application.port.outbound.model.HqSalesOrderKpi;
import com.fallguys.salesservice.domain.model.UserRole;

public interface GetHqSalesOrderKpiUseCase {
    HqSalesOrderKpi getKpi(UserRole role);
}
