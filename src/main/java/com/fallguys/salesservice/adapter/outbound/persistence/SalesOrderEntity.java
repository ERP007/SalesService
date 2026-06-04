package com.fallguys.salesservice.adapter.outbound.persistence;

import com.fallguys.salesservice.domain.model.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Entity
@Table(name = "sales_orders")
@Getter
@NoArgsConstructor
public class SalesOrderEntity {
    @Id
    private String code;

    @Column(name = "from_warehouse_code", nullable = false)
    private String fromWarehouseCode;

    @Column(name = "to_warehouse_code", nullable = false)
    private String toWarehouseCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SalesOrderStatus status;

    @Column(nullable = false)
    private LocalDate desiredArrivalDate;

    @Column(columnDefinition = "text")
    private String requestMemo;

    @Embedded
    private CreationEmbeddable creation;

    @Embedded
    private RequestEmbeddable request;

    @Embedded
    private ApprovalEmbeddable approval;

    @Embedded
    private RejectionEmbeddable rejection;

    @Embedded
    private DeliveryEmbeddable delivery;

    @Embedded
    private CancellationEmbeddable cancellation;

    @BatchSize(size = 50)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "so_code", nullable = false)
    private List<SalesOrderLineEntity> lines = new ArrayList<>();

    public SalesOrder toDomain() {
        List<SalesOrderLine> domainLines = lines.stream()
                .map(l -> l.toDomain(this.code))
                .toList();

        return new SalesOrder(
                code, fromWarehouseCode, toWarehouseCode, status, desiredArrivalDate, requestMemo,
                creation.toDomain(),
                request != null ? request.toDomain() : null,
                approval != null ? approval.toDomain() : null,
                rejection != null ? rejection.toDomain() : null,
                delivery != null ? delivery.toDomain() : null,
                cancellation != null ? cancellation.toDomain() : null,
                domainLines
        );
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
        this.fromWarehouseCode = domain.getFromWarehouseCode();
        this.toWarehouseCode = domain.getToWarehouseCode();
        this.status = domain.getStatus();
        this.desiredArrivalDate = domain.getDesiredArrivalDate();
        this.requestMemo = domain.getRequestMemo();
        this.creation = CreationEmbeddable.from(domain.getCreation());
        this.request = RequestEmbeddable.from(domain.getRequest());
        this.approval = ApprovalEmbeddable.from(domain.getApproval());
        this.rejection = RejectionEmbeddable.from(domain.getRejection());
        this.delivery = DeliveryEmbeddable.from(domain.getDelivery());
        this.cancellation = CancellationEmbeddable.from(domain.getCancellation());
    }
}
