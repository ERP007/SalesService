package com.fallguys.salesservice.domain.model;

import java.time.Instant;

public record SalesOrderRejection(
        String rejectedBy,
        Instant rejectedAt,
        RejectReasonCategory rejectReasonCategory,
        String rejectReasonMemo
) {
    public SalesOrderRejection {
        if (rejectedBy == null || rejectedBy.isBlank()) throw new IllegalArgumentException("반려자 사번은 필수입니다");
        if (rejectedAt == null) throw new IllegalArgumentException("반려 시각은 필수입니다");
        if (rejectReasonCategory == null) throw new IllegalArgumentException("반려 사유 분류는 필수입니다");
    }
}
