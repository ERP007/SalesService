package com.fallguys.salesservice.application.port.outbound;

public record UserInfo(
        String userCode,
        String name,
        String position
) {}
