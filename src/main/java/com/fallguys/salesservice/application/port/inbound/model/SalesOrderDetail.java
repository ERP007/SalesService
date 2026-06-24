package com.fallguys.salesservice.application.port.inbound.model;

import com.fallguys.salesservice.domain.model.ActorRef;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;

/**
 * 발주 상세. 지점·본사 공통 모델. 창고명은 서비스가 채우고(지점은 live, 본사는 스냅샷),
 * requester·approver는 스냅샷에서 읽는다(미요청·미승인은 null).
 */
public record SalesOrderDetail(
        SalesOrder salesOrder,
        String fromWarehouseName,
        String toWarehouseName,
        ActorRef requester,
        ActorRef approver
) {
}
