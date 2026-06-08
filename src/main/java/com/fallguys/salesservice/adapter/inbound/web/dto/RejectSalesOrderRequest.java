package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.domain.model.RejectReasonCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RejectSalesOrderRequest(
        @NotNull(message = "반려 사유 카테고리는 필수입니다") RejectReasonCategory reasonCategory,
        @Size(max = 500, message = "메모는 500자 이하여야 합니다") String memo
) {
}
