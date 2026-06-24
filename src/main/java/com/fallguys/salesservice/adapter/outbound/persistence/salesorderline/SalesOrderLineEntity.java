package com.fallguys.salesservice.adapter.outbound.persistence.salesorderline;

import com.fallguys.salesservice.domain.model.salesorderline.Priority;
import com.fallguys.salesservice.domain.model.salesorderline.SalesOrderLine;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sales_order_lines", indexes = {
        // so_code로 라인을 묶어 조회(EXISTS 검색·@Formula itemCount·라인 로드)하므로 FK 인덱스 필수.
        @Index(name = "idx_sol_so_code", columnList = "so_code")
})
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
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    public SalesOrderLine toDomain(String soCode) {
        return new SalesOrderLine(id, soCode, itemCode, itemNameSnapshot, unitSnapshot,
                quantity, priority);
    }

    public static SalesOrderLineEntity from(SalesOrderLine domain) {
        SalesOrderLineEntity entity = new SalesOrderLineEntity();
        entity.itemCode = domain.getItemCode();
        entity.itemNameSnapshot = domain.getItemNameSnapshot();
        entity.unitSnapshot = domain.getUnitSnapshot();
        entity.quantity = domain.getQuantity();
        entity.priority = domain.getPriority();
        return entity;
    }

    public void update(SalesOrderLine domain) {
        this.quantity = domain.getQuantity();
        this.priority = domain.getPriority();
    }
}
