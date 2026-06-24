package com.fallguys.salesservice.domain.model.salesorderhistory;

import com.fallguys.salesservice.domain.model.ActorRef;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;

import java.time.Instant;

/**
 * 재고 saga 확정 대기 중인 상태 변경. saga가 진행되는 동안만 존재하는 in-flight 데이터다.
 *
 * APPROVED·DELIVERED는 재고 saga가 DONE으로 확정돼야 비로소 발주 이력
 * ({@link SalesOrderStatusHistory})에 남는다. 그 사이 행위자·시각·부가 데이터(payload)를
 * 여기에 staging해 두었다가, saga 성공 시 이력으로 승격하고 실패 시 폐기한다.
 * 발주당 동시에 진행되는 saga는 1건이므로 soCode로 유일하게 식별한다.
 */
public record PendingStatusChange(
        String soCode,
        SalesOrderStatus status,
        ActorRef actor,
        StatusChangePayload payload,
        Instant occurredAt
) {
    /** saga 확정 시 이력으로 승격한다. 시각은 행위 시점(occurredAt)을 그대로 보존한다. */
    public SalesOrderStatusHistory toHistory() {
        return new SalesOrderStatusHistory(soCode, status, actor, payload, occurredAt);
    }
}
