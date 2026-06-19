package com.fallguys.salesservice.application.port.outbound.port;

import com.fallguys.salesservice.application.port.outbound.model.BranchSalesOrderKpi;

public interface LoadBranchSalesOrderKpiPort {
    BranchSalesOrderKpi loadByBranchCode(String warehouseCode);
}
