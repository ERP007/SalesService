package com.fallguys.salesservice.application.port.inbound;

import com.fallguys.salesservice.domain.model.RejectReasonCategory;
import com.fallguys.salesservice.domain.model.UserRole;

public record RejectSalesOrderCommand(
        String soCode,
        String rejectedBy,
        UserRole role,
        RejectReasonCategory reasonCategory,
        String memo
) {
}
