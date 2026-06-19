package com.fallguys.salesservice.adapter.outbound.persistence.salesorderhistory;

import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.salesorderhistory.SalesOrderStatusHistory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "sales_order_status_history")
@Getter
@NoArgsConstructor
public class SalesOrderStatusHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "so_code", nullable = false)
    private String soCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SalesOrderStatus status;

    @Column(name = "actor_code", nullable = false)
    private String actorCode;

    // 상태별 부가 데이터 JSON. 직렬화/역직렬화는 어댑터(ObjectMapper)가 담당.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // payload는 어댑터가 직렬화한 JSON 문자열을 받는다(부가 데이터 없으면 null).
    public static SalesOrderStatusHistoryEntity from(SalesOrderStatusHistory domain, String payloadJson) {
        SalesOrderStatusHistoryEntity entity = new SalesOrderStatusHistoryEntity();
        entity.soCode = domain.soCode();
        entity.status = domain.status();
        entity.actorCode = domain.actorCode();
        entity.payload = payloadJson;
        entity.createdAt = domain.createdAt();
        return entity;
    }
}
