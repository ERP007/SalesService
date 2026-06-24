package com.fallguys.salesservice.application.port.inbound.model;

import com.fallguys.salesservice.domain.model.ActorRef;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorderline.SalesOrderLine;

import java.util.List;

/**
 * 발주 상세. 지점·본사 공통 모델. 창고명·lines의 부품명/단위는 서비스가 표시용으로 채운다
 * (DRAFT는 스냅샷이 없어 live 조회, 확정은 스냅샷 사용). requester·approver는 스냅샷에서 읽는다.
 */
public record SalesOrderDetail(
        SalesOrder salesOrder,
        String fromWarehouseName,
        String toWarehouseName,
        ActorRef requester,
        ActorRef approver,
        List<SalesOrderLine> lines
) {
}
