package com.fallguys.salesservice.domain.model;

import java.time.Instant;

public record SalesOrderDelivery(
        String deliveredBy,
        Instant deliveredAt,
        DiffReasonCategory diffReasonCategory,
        String diffReasonMemo
) {}
