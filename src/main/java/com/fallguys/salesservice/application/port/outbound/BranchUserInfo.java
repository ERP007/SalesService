package com.fallguys.salesservice.application.port.outbound;

public record BranchUserInfo(
        String userId,
        String warehouseCode
) {
}
