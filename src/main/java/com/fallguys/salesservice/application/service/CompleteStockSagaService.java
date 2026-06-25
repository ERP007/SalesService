package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.usecase.CompleteStockSagaUseCase;
import com.fallguys.salesservice.application.port.outbound.port.AppendSalesOrderStatusHistoryPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.PendingStatusChangePort;
import com.fallguys.salesservice.application.port.outbound.port.SaveSalesOrderPort;
import com.fallguys.salesservice.domain.model.salesorder.SagaStatus;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorderhistory.PendingStatusChange;
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
    private final UserActivityRecorder userActivityRecorder;

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

        // 불변식: saga가 진행 중이면 staging은 행위 시점에 같은 트랜잭션으로 저장됐어야 한다.
        // 없으면 데이터 손상/버그이므로 조용히 DONE 확정해 이력을 잃지 말고, 트랜잭션을 롤백해 표면화한다.
        PendingStatusChange pending = pendingStatusChangePort.findBySoCode(soCode)
                .orElseThrow(() -> new IllegalStateException(
                        "saga 확정 시 staging된 상태 변경이 없습니다(불변식 위반): soCode=" + soCode));

        if (saga == SagaStatus.SENDING) {
            order.markSagaProcessing();
        }
        order.markSagaDone();
        saveSalesOrderPort.save(order);

        appendHistoryPort.append(pending.toHistory());
        // saga 확정(approve/deliver) 시점에 사용자 활동도 함께 발행한다(이력과 동일 타이밍).
        userActivityRecorder.statusChanged(
                pending.soCode(), pending.status(), pending.actor().code(), pending.occurredAt());
        pendingStatusChangePort.removeBySoCode(soCode);
    }
}
