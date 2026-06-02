package com.fallguys.salesservice.domain.model;

import java.time.Instant;

public record SalesOrderRequest(
        String requestedBy,
        Instant requestedAt
) {
    public SalesOrderRequest {
        if (requestedBy == null || requestedBy.isBlank()) throw new IllegalArgumentException("요청자 사번은 필수입니다");
        if (requestedAt == null) throw new IllegalArgumentException("요청 시각은 필수입니다");
    }
}
