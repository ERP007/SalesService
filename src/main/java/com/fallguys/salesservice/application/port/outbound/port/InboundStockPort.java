package com.fallguys.salesservice.application.port.outbound.port;

import com.fallguys.salesservice.application.port.outbound.model.Executor;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;

// 재고 서비스에 입고 요청 이벤트를 적재한다(outbox).
public interface InboundStockPort {
    void inbound(SalesOrder order, Executor executor);
}
