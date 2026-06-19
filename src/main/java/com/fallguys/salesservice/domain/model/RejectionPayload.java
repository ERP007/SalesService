package com.fallguys.salesservice.domain.model;

public record RejectionPayload(
        RejectReasonCategory rejectReasonCategory,
        String rejectReasonMemo
) implements StatusChangePayload {
}
