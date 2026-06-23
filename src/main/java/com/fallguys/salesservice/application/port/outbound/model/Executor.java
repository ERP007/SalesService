package com.fallguys.salesservice.application.port.outbound.model;

/**
 * 재고 이벤트 수행자(승인자·입고 처리자). code는 필수, name은 JWT claim 부재 시 null 가능.
 */
public record Executor(
        String code,
        String name
) {
}
