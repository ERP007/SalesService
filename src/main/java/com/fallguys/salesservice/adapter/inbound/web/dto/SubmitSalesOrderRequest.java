package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.application.port.inbound.SubmitSalesOrderCommand;
import com.fallguys.salesservice.application.port.inbound.CreateSalesOrderLineCommand;
import com.fallguys.salesservice.domain.model.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record SubmitSalesOrderRequest(
        @NotBlank(message = "수신 창고 지정은 필수입니다")
        String warehouseCode,

        @NotNull(message = "도착 희망일은 필수입니다")
        LocalDate desiredArrivalDate,

        @Size(max = 500, message = "메모는 500자 이하여야 합니다")
        String memo,

        @NotNull
        @Size(min = 1, max = 50, message = "발주 품목은 1개 이상 50개 이하여야 합니다")
        @Valid
        List<@NotNull(message = "발주 품목에 빈 항목이 포함될 수 없습니다") SubmitSalesOrderLineRequest> lines
) {
    public SubmitSalesOrderCommand toCommand(String soCode, String requestedBy, UserRole role, String requesterWarehouseCode) {
        List<CreateSalesOrderLineCommand> lineCommands = lines.stream()
                .map(SubmitSalesOrderLineRequest::toCommand)
                .toList();
        return new SubmitSalesOrderCommand(soCode, requestedBy, role, requesterWarehouseCode, warehouseCode, desiredArrivalDate, memo, lineCommands);
    }
}
