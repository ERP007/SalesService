package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.usecase.CompleteStockSagaUseCase;
import com.fallguys.salesservice.application.port.outbound.port.AppendSalesOrderStatusHistoryPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.PendingStatusChangePort;
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
    private final AppendSalesOrderStatusHistoryPort appendHistoryPort;
    private final PendingStatusChangePort pendingStatusChangePort;

    /**
     * 성공 응답 수신 시 saga를 DONE으로 종료하고, staging된 상태 변경을 이력으로 승격한다.
     *
     * 흐름:
     * 1) soCode로 발주를 조회한다.
     * 2) saga 상태에 따라 DONE으로 전진시킨다.
     *    - SENDING: 발행 확정(PROCESSING)보다 응답이 먼저 도착한 경우 PROCESSING을 거쳐 DONE
     *    - PROCESSING: 곧바로 DONE
     *    - 그 외(DONE/FAILED/NONE): 중복·무효 응답이므로 무시(멱등)
     * 3) 확정된 상태 변경(APPROVED/DELIVERED)을 staging에서 읽어 이력에 기록하고 staging을 제거한다.
     *    이력은 이 시점(saga 확정)에만 남으므로 되돌려진 시도는 이력에 나타나지 않는다.
     *
     * 트랜잭션: 쓰기(독립). reply listener가 비동기로 호출한다. saga 전진·이력 승격·staging 제거가
     * 한 트랜잭션으로 묶인다. 멱등 처리(saga 상태 가드)로 중복 reply는 무시된다.
     */
    @Override
    @Transactional
    public void complete(String soCode) {
        SalesOrder order = loadSalesOrderPort.load(soCode);
        SagaStatus saga = order.getSagaStatus();
        if (!saga.inProgress()) {
            return;
        }
        if (saga == SagaStatus.SENDING) {
            order.markSagaProcessing();
        }
        order.markSagaDone();
        saveSalesOrderPort.save(order);

        pendingStatusChangePort.findBySoCode(soCode).ifPresent(pending -> {
            appendHistoryPort.append(pending.toHistory());
            pendingStatusChangePort.removeBySoCode(soCode);
        });
    }
}
