package com.fallguys.salesservice.domain.model.salesorderline;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SalesOrderLine {
    private final Long id;
    private final String soCode;
    private final String itemCode;
    private final String itemNameSnapshot;
    private final String unitSnapshot;
    private int quantity;
    private Priority priority;
}
