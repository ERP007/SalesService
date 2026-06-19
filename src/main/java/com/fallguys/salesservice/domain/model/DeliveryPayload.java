package com.fallguys.salesservice.domain.model;

import java.time.LocalDate;

public record DeliveryPayload(
        LocalDate deliveredDate,
        DiffReasonCategory diffReasonCategory,
        String diffReasonMemo
) implements StatusChangePayload {
}
