package com.fallguys.salesservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SalesOrderLine {
    private final Long id;
    private final String soCode;
    private final String itemSku;
    private final String itemNameSnapshot;
    private final String unitSnapshot;
    private int requestedQuantity;
    private Integer approvedQuantity;
    private Integer deliveredQuantity;
    private Priority priority;
}
