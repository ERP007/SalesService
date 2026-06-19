package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.salesorderline.SalesOrderLine;

import java.time.LocalDate;

/**
 * 발주 상태 전환(생성·제출·승인·반려·취소·입고) 공통 응답.
 *
 * 상태별 부가 데이터(승인 송장·반려 사유 등)는 이력 조회로 확인하며, 전환 응답은
 * 결과 상태와 공통 식별 정보만 반환한다.
 */
public record SalesOrderResponse(
        String code,
        String fromWarehouseCode,
        String toWarehouseCode,
        LocalDate desiredArrivalDate,
        SalesOrderStatus status,
        int totalQuantity
) {
    public static SalesOrderResponse from(SalesOrder order) {
        int totalQuantity = order.getLines() != null
                ? order.getLines().stream().mapToInt(SalesOrderLine::getRequestedQuantity).sum()
                : 0;
        return new SalesOrderResponse(
                order.getCode(),
                order.getFromWarehouseCode(),
                order.getToWarehouseCode(),
                order.getDesiredArrivalDate(),
                order.getStatus(),
                totalQuantity
        );
    }
}
