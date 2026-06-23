package com.fallguys.salesservice.domain.model.salesorder;

/**
 * 비동기 재고 saga의 진행 상태(transport 관심사).
 * 비즈니스 마일스톤인 {@link SalesOrderStatus}와 분리한다.
 *
 * NONE       : saga 미시작(DRAFT·REQUESTED 등)
 * SENDING    : outbox 행 작성됨, 릴레이가 아직 발행하지 않음(메시지 발행 대기)
 * PROCESSING : broker 발행 성공, 재고 서비스 처리 결과 대기
 * DONE       : 재고 서비스 성공 응답 수신(saga 정상 종료)
 * FAILED     : 재고 서비스 실패 응답 수신, 보상 트랜잭션 수행됨(saga 비정상 종료)
 */
public enum SagaStatus {
    NONE,
    SENDING,
    PROCESSING,
    DONE,
    FAILED
}
