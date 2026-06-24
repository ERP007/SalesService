package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.application.port.inbound.command.CreateSalesOrderCommand;
import com.fallguys.salesservice.application.port.inbound.command.CreateSalesOrderLineCommand;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.UserRole;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateSalesOrderRequest(
        @NotBlank(message = "창고 지정은 필수입니다")
        String warehouseCode,

        @Size(max = 500, message = "메모는 500자 이하여야 합니다")
        String memo,

        @NotNull
        @Size(min = 1, max = 50, message = "발주 품목은 1개 이상 50개 이하여야 합니다")
        @Valid
        List<@NotNull CreateSalesOrderLineRequest> lines
) {
    public CreateSalesOrderCommand toCommand(String requestedBy, String requesterName, String requesterPosition,
                                             UserRole role, String fromWarehouseCode) {
        List<CreateSalesOrderLineCommand> lineCommands = lines.stream()
                .map(CreateSalesOrderLineRequest::toCommand)
                .toList();
        return new CreateSalesOrderCommand(fromWarehouseCode, warehouseCode, memo,
                SalesOrderStatus.REQUESTED, lineCommands, requestedBy, requesterName, requesterPosition, role);
    }
}
