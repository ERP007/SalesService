package com.fallguys.salesservice.adapter.outbound.client.dto;

import java.util.List;

public record UserBatchResponse(
        List<UserData> users,
        List<String> notFoundEmployeeNumbers
) {
    public record UserData(
            String employeeNumber,
            String name,
            String position
    ) {
    }
}
