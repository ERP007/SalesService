package com.fallguys.salesservice.application.port.inbound.command;

/**
 * 보상 대상 saga 단계.
 * OUTBOUND : 승인(APPROVED) 출고 saga → REQUESTED로 보상
 * INBOUND  : 배송(DELIVERED) 입고 saga → APPROVED로 보상
 */
public enum StockSagaStage {
    OUTBOUND,
    INBOUND
}
