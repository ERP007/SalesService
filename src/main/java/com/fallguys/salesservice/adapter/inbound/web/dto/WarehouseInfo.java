package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.domain.model.WarehouseRef;

public record WarehouseInfo(String code, String name) {
    public static WarehouseInfo from(WarehouseRef ref) {
        if (ref == null) return null;
        return new WarehouseInfo(ref.code(), ref.nameSnapshot());
    }
}
