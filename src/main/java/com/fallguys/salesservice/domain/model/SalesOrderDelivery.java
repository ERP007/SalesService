package com.fallguys.salesservice.domain.model;

import java.time.Instant;
import java.time.LocalDate;

public record SalesOrderDelivery(
        String deliveredBy,
        LocalDate deliveredDate,
        Instant deliveredAt,
        DiffReasonCategory diffReasonCategory,
        String diffReasonMemo
) {
}
