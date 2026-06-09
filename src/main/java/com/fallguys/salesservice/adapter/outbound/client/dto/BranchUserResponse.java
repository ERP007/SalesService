package com.fallguys.salesservice.adapter.outbound.client.dto;

// TODO: User 서비스 응답 스펙 확정 후 필드 보완
public record BranchUserResponse(
        String userCode,
        String warehouseCode
) {
}
