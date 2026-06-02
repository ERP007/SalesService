package com.fallguys.salesservice.domain.model;

import java.time.Instant;

public record SalesOrderCancellation(
        String canceledBy,
        Instant canceledAt,
        String cancelReason
) {
    public SalesOrderCancellation {
        if (canceledBy == null || canceledBy.isBlank()) throw new IllegalArgumentException("취소자 사번은 필수입니다");
        if (canceledAt == null) throw new IllegalArgumentException("취소 시각은 필수입니다");
    }
}
