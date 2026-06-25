package com.fallguys.salesservice.application.port.outbound.port;

import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;

// 재고 서비스에 출고를 동기 REST로 요청한다(부하 테스트용 sync 경로). 실패 시 예외로 호출자 트랜잭션을 롤백.
public interface SyncOutboundStockPort {
    void outbound(SalesOrder order);
}
