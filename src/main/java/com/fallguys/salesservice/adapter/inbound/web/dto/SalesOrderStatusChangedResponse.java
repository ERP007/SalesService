package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.salesorderline.SalesOrderLine;

/**
 * 발주 상태 전환(생성·제출·승인·반려·취소·입고) 공통 응답.
 *
 * 상태별 부가 데이터(승인 송장·반려 사유 등)는 이력 조회로 확인하며, 전환 응답은
 * 결과 상태와 공통 식별 정보만 반환한다.
 */
public record SalesOrderStatusChangedResponse(
        String code,
        String fromWarehouseCode,
        String toWarehouseCode,
        SalesOrderStatus status,
        String progress,
        int totalQuantity
) {
    public static SalesOrderStatusChangedResponse from(SalesOrder order) {
        int totalQuantity = order.getLines() != null
                ? order.getLines().stream().mapToInt(SalesOrderLine::getQuantity).sum()
                : 0;
        return new SalesOrderStatusChangedResponse(
                order.getCode(),
                order.getFrom().code(),
                order.getTo().code(),
                order.getStatus(),
                order.progress().name(),
                totalQuantity
        );
    }
}
