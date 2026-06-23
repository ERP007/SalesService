package com.fallguys.salesservice.application.port.inbound.command;

/**
 * 재고 saga 실패 보상 입력.
 * - soCode : 보상 대상 발주
 * - stage  : 출고/입고 단계
 * - reason : 재고 서비스 실패 사유(errorCode + 메시지). 로깅용.
 */
public record CompensateStockSagaCommand(
        String soCode,
        StockSagaStage stage,
        String reason
) {
}
