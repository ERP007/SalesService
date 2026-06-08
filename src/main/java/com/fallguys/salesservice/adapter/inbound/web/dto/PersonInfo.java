package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.application.port.outbound.UserInfo;

public record PersonInfo(String code, String name, String position) {
    public static PersonInfo from(UserInfo info) {
        if (info == null) return null;
        return new PersonInfo(info.userCode(), info.name(), info.position());
    }
}
