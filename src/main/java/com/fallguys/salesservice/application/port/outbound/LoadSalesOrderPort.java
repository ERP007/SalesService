package com.fallguys.salesservice.application.port.outbound;

import com.fallguys.salesservice.domain.model.SalesOrder;

// SO 코드로 발주를 조회한다.
// 미존재 시 ResourceNotFoundException(SO-06-01) 발생
public interface LoadSalesOrderPort {
    SalesOrder load(String soCode);
}
