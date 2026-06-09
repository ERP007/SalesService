package com.fallguys.salesservice.adapter.outbound.client.dto;

public record WarehouseResponse(
        Long id,
        String code,
        String name,
        String type,
        String branchName,
        boolean active
) {
}
