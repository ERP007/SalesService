package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.usecase.ConfirmStockEventPublishedUseCase;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.SaveSalesOrderPort;
import com.fallguys.salesservice.domain.model.salesorder.SagaStatus;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConfirmStockEventPublishedService implements ConfirmStockEventPublishedUseCase {

    private final LoadSalesOrderPort loadSalesOrderPort;
    private final SaveSalesOrderPort saveSalesOrderPort;

    /**
     * 발행 확정 시 saga를 SENDING → PROCESSING으로 전진시킨다.
     *
     * 흐름:
     * 1) soCode로 발주를 조회한다.
     * 2) sagaStatus가 SENDING일 때만 PROCESSING으로 전환하고 저장한다.
     *
     * 트랜잭션: 쓰기(독립). relay가 메시지 발행 확정 후 별도 트랜잭션으로 호출한다.
     *
     * 멱등: 폴러·AFTER_COMMIT 이중 발행으로 중복 호출될 수 있으므로
     * SENDING이 아니면(이미 PROCESSING/DONE/FAILED) 아무 것도 하지 않는다.
     */
    @Override
    @Transactional
    public void confirmPublished(String soCode) {
        SalesOrder order = loadSalesOrderPort.load(soCode);
        if (order.getSagaStatus() == SagaStatus.SENDING) {
            order.markSagaProcessing();
            saveSalesOrderPort.save(order);
        }
    }
}
