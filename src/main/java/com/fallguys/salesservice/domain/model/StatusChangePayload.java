package com.fallguys.salesservice.domain.model;

/**
 * 상태 변경 이력의 상태별 부가 데이터.
 *
 * actor·시각·대상 SO는 이력 공통 컬럼이 보관하고, 이 타입은 각 상태 전환에만
 * 존재하는 부가 정보만 담는다. CREATED·REQUESTED처럼 부가 데이터가 없는 전환은
 * payload를 갖지 않는다(null).
 */
public sealed interface StatusChangePayload
        permits ApprovalPayload, RejectionPayload, DeliveryPayload, CancellationPayload {
}
