package com.fallguys.salesservice.adapter.outbound.persistence.salesorder;

import com.fallguys.salesservice.adapter.outbound.persistence.salesorderline.SalesOrderLineEntity;
import com.fallguys.salesservice.domain.model.salesorder.BranchSalesOrderSummary;
import com.fallguys.salesservice.domain.model.salesorder.HqSalesOrderSummary;
import com.fallguys.salesservice.domain.model.salesorder.OrderProgress;
import com.fallguys.salesservice.domain.model.salesorder.SagaStatus;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderRequest;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.salesorderline.SalesOrderLine;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Formula;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Entity
@Table(name = "sales_orders", indexes = {
        // 지점 목록: from_warehouse_code 필터 + requested_at 정렬/범위
        @Index(name = "idx_so_from_warehouse_requested_at", columnList = "from_warehouse_code, requested_at"),
        // 본사 목록: 창고 미지정 시 requested_at 범위/정렬
        @Index(name = "idx_so_requested_at", columnList = "requested_at"),
        // 상태 집계(KPI)·status IN 필터
        @Index(name = "idx_so_status", columnList = "status")
})
@Getter
@NoArgsConstructor
public class SalesOrderEntity {
    @Id
    private String code;

    @Version
    @Column(nullable = false)
    private Long version;

    // 창고명 스냅샷. DRAFT는 code만 들고 name은 null, REQUESTED 확정 시 박제.
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "from_warehouse_code", nullable = false)),
            @AttributeOverride(name = "name", column = @Column(name = "from_warehouse_name"))
    })
    private WarehouseEmbeddable from;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "to_warehouse_code", nullable = false)),
            @AttributeOverride(name = "name", column = @Column(name = "to_warehouse_name"))
    })
    private WarehouseEmbeddable to;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SalesOrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "saga_status", nullable = false)
    private SagaStatus sagaStatus;

    @Column(columnDefinition = "text")
    private String requestMemo;

    // 직전 재고 saga 실패 사유(보상 시 기록). 진행 페이지 노출용.
    @Column(name = "last_failure_reason", columnDefinition = "text")
    private String lastFailureReason;

    @Embedded
    private CreationEmbeddable creation;

    @Embedded
    private RequestEmbeddable request;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "so_code", nullable = false)
    private List<SalesOrderLineEntity> lines = new ArrayList<>();

    // 발주 라인 수. 목록(summary) 조회 시 lines 컬렉션을 로드하지 않고 스칼라로 함께 읽기 위해
    // 서브쿼리로 계산한다(읽기 전용).
    @Formula("(select count(*) from sales_order_lines l where l.so_code = code)")
    private int itemCount;

    public SalesOrder toDomain() {
        List<SalesOrderLine> domainLines = lines.stream()
                .map(l -> l.toDomain(this.code))
                .toList();

        return new SalesOrder(
                code, from.toDomain(), to.toDomain(), status, sagaStatus, requestMemo,
                creation.toDomain(),
                request != null ? request.toDomain() : null,
                domainLines,
                lastFailureReason
        );
    }

    // 목록 조회용 요약 변환. itemCount는 @Formula 스칼라라 lines 컬렉션 로드를 유발하지 않는다.
    // 요청 정보는 SalesOrderRequest로 묶어 넘긴다(미요청 DRAFT는 null).
    public BranchSalesOrderSummary toBranchSummary() {
        return new BranchSalesOrderSummary(
                code, status, OrderProgress.from(status, sagaStatus), requestSnapshot(), itemCount);
    }

    public HqSalesOrderSummary toHqSummary() {
        return new HqSalesOrderSummary(
                code, from.toDomain(), status, OrderProgress.from(status, sagaStatus), requestSnapshot(), itemCount);
    }

    private SalesOrderRequest requestSnapshot() {
        return request != null ? request.toDomain() : null;
    }

    public static SalesOrderEntity from(SalesOrder domain) {
        SalesOrderEntity entity = new SalesOrderEntity();
        entity.applyDomain(domain);
        entity.lines = domain.getLines().stream()
                .map(SalesOrderLineEntity::from)
                .collect(Collectors.toCollection(ArrayList::new));
        return entity;
    }

    public SalesOrderEntity update(SalesOrder domain) {
        applyDomain(domain);
        Map<Long, SalesOrderLineEntity> lineMap = lines.stream()
                .filter(l -> l.getId() != null)
                .collect(Collectors.toMap(SalesOrderLineEntity::getId, l -> l));
        List<SalesOrderLineEntity> updated = new ArrayList<>();
        domain.getLines().forEach(domainLine -> {
            SalesOrderLineEntity existing =
                    domainLine.getId() != null ? lineMap.get(domainLine.getId()) : null;
            if (existing != null) {
                existing.update(domainLine);
                updated.add(existing);
            } else {
                updated.add(SalesOrderLineEntity.from(domainLine));
            }
        });
        lines.clear();
        lines.addAll(updated);
        return this;
    }

    private void applyDomain(SalesOrder domain) {
        this.code = domain.getCode();
        this.from = WarehouseEmbeddable.from(domain.getFrom());
        this.to = WarehouseEmbeddable.from(domain.getTo());
        this.status = domain.getStatus();
        this.sagaStatus = domain.getSagaStatus();
        this.requestMemo = domain.getRequestMemo();
        this.lastFailureReason = domain.getLastFailureReason();
        this.creation = CreationEmbeddable.from(domain.getCreation());
        this.request = RequestEmbeddable.from(domain.getRequest());
    }
}
