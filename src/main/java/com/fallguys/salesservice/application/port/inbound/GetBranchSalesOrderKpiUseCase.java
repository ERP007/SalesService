package com.fallguys.salesservice.application.port.inbound;

import com.fallguys.salesservice.application.port.outbound.BranchSalesOrderKpi;
import com.fallguys.salesservice.domain.model.UserRole;

public interface GetBranchSalesOrderKpiUseCase {
    BranchSalesOrderKpi getKpi(String warehouseCode, UserRole role);
}
