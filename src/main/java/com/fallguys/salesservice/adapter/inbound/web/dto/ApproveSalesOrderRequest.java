package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.application.port.inbound.command.ApproveSalesOrderCommand;
import com.fallguys.salesservice.domain.model.salesorderhistory.CarrierType;
import com.fallguys.salesservice.domain.model.UserRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public record ApproveSalesOrderRequest(
        @NotNull(message = "승인일은 필수입니다")
        @PastOrPresent(message = "승인일은 오늘 또는 과거여야 합니다")
        LocalDate approvedDate,

        @NotNull(message = "운송 수단은 필수입니다") CarrierType carrierType,

        String invoiceNumber
) {
    public ApproveSalesOrderCommand toCommand(String soCode, String approvedBy, UserRole role) {
        return new ApproveSalesOrderCommand(soCode, approvedBy, role, approvedDate, carrierType, invoiceNumber);
    }
}
