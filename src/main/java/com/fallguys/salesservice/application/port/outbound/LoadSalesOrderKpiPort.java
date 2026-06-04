package com.fallguys.salesservice.application.port.outbound;

public interface LoadSalesOrderKpiPort {
    SalesOrderKpi loadByBranchCode(String branchCode);
}
