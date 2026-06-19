package com.fallguys.salesservice.application.port.inbound.model;

import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorderhistory.CarrierType;

import java.time.Instant;

/**
 * 지점 발주 상세. 승인 부가 데이터(approvedAt·invoiceNumber·carrierType)는 상태 변경 이력의
 * APPROVED 행에서 가져온다(미승인 발주는 null).
 */
public record SalesOrderDetail(
        SalesOrder salesOrder,
        String fromWarehouseName,
        String toWarehouseName,
        Instant approvedAt,
        String invoiceNumber,
        CarrierType carrierType
) {
}
