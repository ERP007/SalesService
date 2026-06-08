package com.fallguys.salesservice.adapter.outbound.client.dto;

public record UserInfoResponse(
        String userCode,
        String name,
        String position
) {}
