package com.fallguys.salesservice.domain.model.salesorder;

import com.fallguys.salesservice.domain.model.salesorderhistory.DiffReasonCategory;

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
