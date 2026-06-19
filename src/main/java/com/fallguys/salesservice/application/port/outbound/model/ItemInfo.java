package com.fallguys.salesservice.application.port.outbound.model;

public record ItemInfo(
        String itemCode,
        String itemName,
        String unit
) {
}
