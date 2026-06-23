package com.fallguys.salesservice.application.port.outbound.model;

public record UserInfo(
        String userCode,
        String name,
        String position
) {}
