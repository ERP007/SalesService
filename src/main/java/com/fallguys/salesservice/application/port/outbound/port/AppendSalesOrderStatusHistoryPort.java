package com.fallguys.salesservice.application.port.outbound.port;

import com.fallguys.salesservice.domain.model.salesorderhistory.SalesOrderStatusHistory;

// 발주 상태 변경 이력 1건을 append 한다. SO와 같은 쓰기 트랜잭션에 참여.
public interface AppendSalesOrderStatusHistoryPort {
    void append(SalesOrderStatusHistory history);
}
