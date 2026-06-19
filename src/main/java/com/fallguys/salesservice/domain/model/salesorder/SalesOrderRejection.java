package com.fallguys.salesservice.domain.model.salesorder;

import com.fallguys.salesservice.domain.model.salesorderhistory.RejectReasonCategory;

import java.time.Instant;

public record SalesOrderRejection(
        String rejectedBy,
        Instant rejectedAt,
        RejectReasonCategory rejectReasonCategory,
        String rejectReasonMemo
) {
}
