package com.fallguys.salesservice.application.port.outbound;

public record ItemInfo(
        String itemCode,
        String itemName,
        String unit
) {
}
