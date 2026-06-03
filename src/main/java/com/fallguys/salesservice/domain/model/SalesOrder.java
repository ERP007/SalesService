package com.fallguys.salesservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class SalesOrder {

    private final String code;
    private final String fromWarehouseCode;
    private final String toWarehouseCode;
    private SalesOrderStatus status;
    private LocalDate desiredArrivalDate;
    private String requestMemo;

    private final SalesOrderCreation creation;
    private SalesOrderRequest request;
    private SalesOrderApproval approval;
    private SalesOrderRejection rejection;
    private SalesOrderDelivery delivery;
    private SalesOrderCancellation cancellation;

    private final List<SalesOrderLine> lines;
}
