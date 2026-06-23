package com.fallguys.salesservice.domain.model.salesorderhistory;

import com.fallguys.salesservice.domain.model.ActorRef;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;

import java.time.Instant;

/**
 * 발주 상태 변경 이력 한 건. append-only 감사 로그.
 *
 * SalesOrder와 연관관계를 두지 않고 soCode로만 느슨하게 연결한다. 한 번 기록되면
 * 수정·삭제하지 않으므로 식별자 없이 값으로 다룬다(영속 surrogate id는 어댑터가 보관).
 *
 * actor는 행위자 참조로, code는 항상 보관하고 name·position은 확정 시점 스냅샷이다.
 * payload는 상태별 부가 데이터로, 부가 정보가 없는 전환(CREATED·REQUESTED)은 null이다.
 */
public record SalesOrderStatusHistory(
        String soCode,
        SalesOrderStatus status,
        ActorRef actor,
        StatusChangePayload payload,
        Instant createdAt
) {
    public static SalesOrderStatusHistory of(String soCode,
                                             SalesOrderStatus status,
                                             ActorRef actor,
                                             Instant createdAt) {
        return new SalesOrderStatusHistory(soCode, status, actor, null, createdAt);
    }

    public static SalesOrderStatusHistory of(String soCode,
                                             SalesOrderStatus status,
                                             ActorRef actor,
                                             StatusChangePayload payload,
                                             Instant createdAt) {
        return new SalesOrderStatusHistory(soCode, status, actor, payload, createdAt);
    }
}
