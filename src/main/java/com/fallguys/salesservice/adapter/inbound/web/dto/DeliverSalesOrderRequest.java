package com.fallguys.salesservice.adapter.inbound.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public record DeliverSalesOrderRequest(
        @NotNull(message = "도착일은 필수입니다")
        @PastOrPresent(message = "도착일은 오늘 또는 과거여야 합니다")
        LocalDate deliveredDate
) {
}
