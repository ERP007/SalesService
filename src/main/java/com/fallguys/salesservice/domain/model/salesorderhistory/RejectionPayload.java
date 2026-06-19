package com.fallguys.salesservice.domain.model.salesorderhistory;

public record RejectionPayload(
        RejectReasonCategory rejectReasonCategory,
        String rejectReasonMemo
) implements StatusChangePayload {
}
