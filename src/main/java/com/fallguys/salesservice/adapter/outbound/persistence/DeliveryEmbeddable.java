package com.fallguys.salesservice.adapter.outbound.persistence;

import com.fallguys.salesservice.domain.model.DiffReasonCategory;
import com.fallguys.salesservice.domain.model.SalesOrderDelivery;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.Instant;

@Embeddable
public record DeliveryEmbeddable(
        String deliveredBy,
        Instant deliveredAt,
        @Enumerated(EnumType.STRING) DiffReasonCategory diffReasonCategory,
        String diffReasonMemo
) {
    public static DeliveryEmbeddable from(SalesOrderDelivery domain) {
        if (domain == null) return null;
        return new DeliveryEmbeddable(domain.deliveredBy(), domain.deliveredAt(),
                domain.diffReasonCategory(), domain.diffReasonMemo());
    }

    public SalesOrderDelivery toDomain() {
        if (deliveredBy == null) return null;
        return new SalesOrderDelivery(deliveredBy, deliveredAt, diffReasonCategory, diffReasonMemo);
    }
}
