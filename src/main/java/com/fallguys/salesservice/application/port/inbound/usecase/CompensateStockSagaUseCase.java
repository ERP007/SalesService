package com.fallguys.salesservice.application.port.inbound.usecase;

import com.fallguys.salesservice.application.port.inbound.command.CompensateStockSagaCommand;

/**
 * 재고 서비스 실패 응답(rejected) 수신 시 비즈니스 상태를 되돌리고 saga를 FAILED로 종료한다.
 */
public interface CompensateStockSagaUseCase {
    void compensate(CompensateStockSagaCommand command);
}
