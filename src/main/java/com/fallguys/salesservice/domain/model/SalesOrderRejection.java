package com.fallguys.salesservice.domain.model;

import java.time.Instant;

public record SalesOrderRejection(
        String rejectedBy,
        Instant rejectedAt,
        RejectReasonCategory rejectReasonCategory,
        String rejectReasonMemo
) {}
