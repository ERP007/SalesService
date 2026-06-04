package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.application.port.inbound.CreateSalesOrderLineCommand;
import com.fallguys.salesservice.domain.model.Priority;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmitSalesOrderLineRequest(
        @NotBlank(message = "부품 코드는 필수입니다")
        String itemCode,

        @Min(value = 1, message = "부품 수량은 1개 이상이어야 합니다")
        int quantity,

        @NotNull(message = "우선 순위는 필수입니다")
        Priority priority
) {
    public CreateSalesOrderLineCommand toCommand() {
        return new CreateSalesOrderLineCommand(itemCode, quantity, priority);
    }
}
