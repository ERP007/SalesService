package com.fallguys.salesservice.application.port.outbound.port;

import com.fallguys.salesservice.domain.model.salesorderhistory.SalesOrderStatusHistory;

import java.util.List;

// SO 코드로 상태 변경 이력을 최신순(created_at DESC)으로 조회한다.
public interface LoadSalesOrderStatusHistoryPort {
    List<SalesOrderStatusHistory> loadBySoCode(String soCode);
}
