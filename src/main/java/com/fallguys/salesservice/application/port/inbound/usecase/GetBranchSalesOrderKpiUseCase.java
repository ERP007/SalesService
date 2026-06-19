package com.fallguys.salesservice.application.port.inbound.usecase;

import com.fallguys.salesservice.application.port.outbound.model.BranchSalesOrderKpi;
import com.fallguys.salesservice.domain.model.UserRole;

public interface GetBranchSalesOrderKpiUseCase {
    BranchSalesOrderKpi getKpi(String warehouseCode, UserRole role);
}
