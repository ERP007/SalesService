package com.fallguys.salesservice.application.port.inbound.command;

import com.fallguys.salesservice.domain.model.salesorderhistory.RejectReasonCategory;
import com.fallguys.salesservice.domain.model.UserRole;

public record RejectSalesOrderCommand(
        String soCode,
        String rejectedBy,
        String rejectedByName,
        String rejectedByPosition,
        UserRole role,
        RejectReasonCategory reasonCategory,
        String memo
) {
}
