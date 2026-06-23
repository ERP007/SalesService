package com.fallguys.salesservice.adapter.outbound.persistence.outbox;

/**
 * outbox 행의 발행 상태.
 * PENDING   : 작성됨, 아직 broker 발행 안 됨
 * PUBLISHED : broker 발행 성공
 * FAILED    : 발행 재시도 한계 초과(수동 개입 대상)
 */
public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
