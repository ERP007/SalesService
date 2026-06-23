package com.fallguys.salesservice.domain.model.salesorderhistory;

public record CancellationPayload(
        String cancelReason
) implements StatusChangePayload {
}
