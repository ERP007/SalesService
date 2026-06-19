package com.fallguys.salesservice.application.port.outbound.port;

import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;

// SO 코드로 발주를 조회한다.
// 미존재 시 ResourceNotFoundException(SO-014) 발생
public interface LoadSalesOrderPort {
    SalesOrder load(String soCode);
}
