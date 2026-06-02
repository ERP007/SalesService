package com.fallguys.salesservice.domain.model;

import java.time.Instant;

public record SalesOrderApproval(
        String approvedBy,
        Instant approvedAt,
        String fromWarehouseCode,
        String carrierType,
        String invoiceNumber
) {
    public SalesOrderApproval {
        if (approvedBy == null || approvedBy.isBlank()) throw new IllegalArgumentException("승인자 사번은 필수입니다");
        if (approvedAt == null) throw new IllegalArgumentException("승인 시각은 필수입니다");
        if (fromWarehouseCode == null || fromWarehouseCode.isBlank()) throw new IllegalArgumentException("출고 창고 코드는 필수입니다");
    }
}
