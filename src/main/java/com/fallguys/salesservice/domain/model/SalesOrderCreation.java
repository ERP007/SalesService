package com.fallguys.salesservice.domain.model;

import java.time.Instant;

public record SalesOrderCreation(
        String createdBy,
        Instant createdAt
) {
    public SalesOrderCreation {
        if (createdBy == null || createdBy.isBlank()) throw new IllegalArgumentException("생성자 사번은 필수입니다");
        if (createdAt == null) throw new IllegalArgumentException("생성 시각은 필수입니다");
    }
}
