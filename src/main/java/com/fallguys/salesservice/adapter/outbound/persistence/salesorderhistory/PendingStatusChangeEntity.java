package com.fallguys.salesservice.adapter.outbound.persistence.salesorderhistory;

import com.fallguys.salesservice.domain.model.ActorRef;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.salesorderhistory.PendingStatusChange;
import com.fallguys.salesservice.domain.model.salesorderhistory.StatusChangePayload;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * 재고 saga 확정 대기 상태 변경의 staging 테이블. 발주당 1건이므로 so_code가 PK.
 * saga DONE 시 이력으로 승격 후 삭제, FAILED 시 삭제된다.
 */
@Entity
@Table(name = "pending_status_change")
@Getter
@NoArgsConstructor
public class PendingStatusChangeEntity {

    @Id
    @Column(name = "so_code")
    private String soCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SalesOrderStatus status;

    @Column(name = "actor_code", nullable = false)
    private String actorCode;

    @Column(name = "actor_name")
    private String actorName;

    @Column(name = "actor_position")
    private String actorPosition;

    // 상태별 부가 데이터 JSON. 직렬화/역직렬화는 어댑터(ObjectMapper)가 담당.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload")
    private String payload;

    // 행위 시점(occurredAt). 이력 승격 시 그대로 created_at으로 보존한다.
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public static PendingStatusChangeEntity from(PendingStatusChange domain, String payloadJson) {
        PendingStatusChangeEntity entity = new PendingStatusChangeEntity();
        entity.soCode = domain.soCode();
        entity.status = domain.status();
        entity.actorCode = domain.actor().code();
        entity.actorName = domain.actor().nameSnapshot();
        entity.actorPosition = domain.actor().positionSnapshot();
        entity.payload = payloadJson;
        entity.occurredAt = domain.occurredAt();
        return entity;
    }

    public PendingStatusChange toDomain(StatusChangePayload payload) {
        return new PendingStatusChange(
                soCode, status, ActorRef.of(actorCode, actorName, actorPosition), payload, occurredAt);
    }
}
