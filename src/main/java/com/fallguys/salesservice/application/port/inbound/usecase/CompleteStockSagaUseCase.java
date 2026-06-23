package com.fallguys.salesservice.application.port.inbound.usecase;

/**
 * 재고 서비스 성공 응답(applied) 수신 시 saga를 DONE으로 종료한다.
 */
public interface CompleteStockSagaUseCase {
    void complete(String soCode);
}
