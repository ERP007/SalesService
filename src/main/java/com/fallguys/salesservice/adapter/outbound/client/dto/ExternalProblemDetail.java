package com.fallguys.salesservice.adapter.outbound.client.dto;

public record ExternalProblemDetail(
        String errorCode,
        String detail
) {
}
