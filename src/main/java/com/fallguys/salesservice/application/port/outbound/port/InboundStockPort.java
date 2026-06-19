package com.fallguys.salesservice.application.port.outbound.port;

import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;

// 재고 서비스에 입고 이력을 기록한다.
// 실패 시 ExternalServiceException 또는 SalesOrderException 발생
public interface InboundStockPort {
    void inbound(SalesOrder order);
}
