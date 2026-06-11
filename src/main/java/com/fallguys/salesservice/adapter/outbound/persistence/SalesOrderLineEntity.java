package com.fallguys.salesservice.adapter.outbound.persistence;

import com.fallguys.salesservice.domain.model.Priority;
import com.fallguys.salesservice.domain.model.SalesOrderLine;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sales_order_lines")
@Getter
@NoArgsConstructor
public class SalesOrderLineEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK 컬럼 읽기 전용 매핑. 쓰기는 SalesOrderEntity의 @JoinColumn(name = "so_code")가 담당.
    // JPQL에서 SalesOrderLineEntity.soCode 직접 참조용 (MEMBER OF 대신 JOIN 명시).
    @Column(name = "so_code", nullable = false, insertable = false, updatable = false)
    private String soCode;

    @Column(nullable = false)
    private String itemCode;

    private String itemNameSnapshot;

    private String unitSnapshot;

    @Column(nullable = false)
    private int requestedQuantity;

    private Integer approvedQuantity;

    private Integer deliveredQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    public SalesOrderLine toDomain(String soCode) {
        return new SalesOrderLine(id, soCode, itemCode, itemNameSnapshot, unitSnapshot,
                requestedQuantity, approvedQuantity, deliveredQuantity, priority);
    }

    public static SalesOrderLineEntity from(SalesOrderLine domain) {
        SalesOrderLineEntity entity = new SalesOrderLineEntity();
        entity.itemCode = domain.getItemCode();
        entity.itemNameSnapshot = domain.getItemNameSnapshot();
        entity.unitSnapshot = domain.getUnitSnapshot();
        entity.requestedQuantity = domain.getRequestedQuantity();
        entity.approvedQuantity = domain.getApprovedQuantity();
        entity.deliveredQuantity = domain.getDeliveredQuantity();
        entity.priority = domain.getPriority();
        return entity;
    }

    public SalesOrderLineEntity update(SalesOrderLine domain) {
        this.requestedQuantity = domain.getRequestedQuantity();
        this.approvedQuantity = domain.getApprovedQuantity();
        this.deliveredQuantity = domain.getDeliveredQuantity();
        this.priority = domain.getPriority();
        return this;
    }
}
