package com.fallguys.salesservice.domain.model.salesorderhistory;

import java.time.LocalDate;

public record DeliveryPayload(
        LocalDate deliveredDate
) implements StatusChangePayload {
}
