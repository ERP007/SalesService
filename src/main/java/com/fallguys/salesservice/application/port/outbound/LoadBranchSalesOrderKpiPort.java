package com.fallguys.salesservice.application.port.outbound;

public interface LoadBranchSalesOrderKpiPort {
    BranchSalesOrderKpi loadByBranchCode(String warehouseCode);
}
