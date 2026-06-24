package com.fallguys.salesservice.adapter.outbound.persistence.salesorder;

import com.fallguys.salesservice.domain.model.WarehouseRef;
import jakarta.persistence.Embeddable;

/**
 * 창고 참조 임베더블. code는 항상, name은 확정 시점 스냅샷(DRAFT는 null).
 * from·to 두 번 임베드하므로 컬럼명은 @AttributeOverrides로 분리한다.
 */
@Embeddable
public record WarehouseEmbeddable(
        String code,
        String name
) {
    public static WarehouseEmbeddable from(WarehouseRef ref) {
        return new WarehouseEmbeddable(ref.code(), ref.nameSnapshot());
    }

    public WarehouseRef toDomain() {
        return WarehouseRef.of(code, name);
    }
}
