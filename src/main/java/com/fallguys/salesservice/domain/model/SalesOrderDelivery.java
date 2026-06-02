package com.fallguys.salesservice.domain.model;

import java.time.Instant;

public record SalesOrderDelivery(
        String deliveredBy,
        Instant deliveredAt,
        DiffReasonCategory diffReasonCategory,
        String diffReasonMemo
) {
    public SalesOrderDelivery {
        if (deliveredBy == null || deliveredBy.isBlank()) throw new IllegalArgumentException("입고 처리자 사번은 필수입니다");
        if (deliveredAt == null) throw new IllegalArgumentException("입고 시각은 필수입니다");
    }
}
