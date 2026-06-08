package com.fallguys.salesservice.application.port.outbound;

import com.fallguys.salesservice.domain.model.SalesOrder;

// 재고 서비스에 출고 이력을 기록한다.
// 실패 시 ExternalServiceException 또는 SalesOrderException 발생
public interface OutboundStockPort {
    void outbound(SalesOrder order);
}
