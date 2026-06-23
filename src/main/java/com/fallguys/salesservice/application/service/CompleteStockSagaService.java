package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.usecase.CompleteStockSagaUseCase;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.SaveSalesOrderPort;
import com.fallguys.salesservice.domain.model.salesorder.SagaStatus;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompleteStockSagaService implements CompleteStockSagaUseCase {

    private final LoadSalesOrderPort loadSalesOrderPort;
    private final SaveSalesOrderPort saveSalesOrderPort;

    /**
     * 성공 응답 수신 시 saga를 DONE으로 종료한다.
     *
     * 흐름:
     * 1) soCode로 발주를 조회한다.
     * 2) saga 상태에 따라 DONE으로 전진시킨다.
     *    - PROCESSING: 곧바로 DONE
     *    - SENDING: 발행 확정(PROCESSING) 처리보다 응답이 먼저 도착한 경우 PROCESSING을 거쳐 DONE
     *    - 그 외(DONE/FAILED/NONE): 중복·무효 응답이므로 무시(멱등)
     *
     * 트랜잭션: 쓰기(독립). reply listener가 비동기로 호출한다.
     */
    @Override
    @Transactional
    public void complete(String soCode) {
        SalesOrder order = loadSalesOrderPort.load(soCode);
        SagaStatus saga = order.getSagaStatus();
        if (saga == SagaStatus.SENDING) {
            order.markSagaProcessing();
            order.markSagaDone();
            saveSalesOrderPort.save(order);
        } else if (saga == SagaStatus.PROCESSING) {
            order.markSagaDone();
            saveSalesOrderPort.save(order);
        }
    }
}
