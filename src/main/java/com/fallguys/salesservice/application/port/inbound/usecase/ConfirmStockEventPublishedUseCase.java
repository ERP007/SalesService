package com.fallguys.salesservice.application.port.inbound.usecase;

/**
 * outbox 메시지가 broker로 발행 확정되었을 때 saga를 SENDING → PROCESSING으로 전진시킨다.
 * relay(발행 어댑터)가 호출한다.
 */
public interface ConfirmStockEventPublishedUseCase {
    void confirmPublished(String soCode);
}
