package com.fallguys.salesservice.adapter.inbound.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelSalesOrderRequest(
        @NotBlank(message = "취소 사유는 필수입니다") String reason
) {
}
