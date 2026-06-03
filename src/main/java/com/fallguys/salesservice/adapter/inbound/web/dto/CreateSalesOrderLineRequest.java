package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.application.port.inbound.CreateSalesOrderLineCommand;
import com.fallguys.salesservice.domain.model.Priority;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSalesOrderLineRequest(
        @NotBlank String itemCode,
        @Min(1) int quantity,
        @NotNull Priority priority
) {
    public CreateSalesOrderLineCommand toCommand() {
        return new CreateSalesOrderLineCommand(itemCode, quantity, priority);
    }
}
