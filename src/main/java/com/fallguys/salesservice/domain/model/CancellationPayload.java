package com.fallguys.salesservice.domain.model;

public record CancellationPayload(
        String cancelReason
) implements StatusChangePayload {
}
