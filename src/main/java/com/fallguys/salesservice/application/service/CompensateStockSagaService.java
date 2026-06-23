package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.command.CompensateStockSagaCommand;
import com.fallguys.salesservice.application.port.inbound.usecase.CompensateStockSagaUseCase;
import com.fallguys.salesservice.application.port.outbound.port.AppendSalesOrderStatusHistoryPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.SaveSalesOrderPort;
import com.fallguys.salesservice.domain.model.ActorRef;
import com.fallguys.salesservice.domain.model.salesorder.SagaStatus;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorderhistory.SalesOrderStatusHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompensateStockSagaService implements CompensateStockSagaUseCase {

    private static final String SYSTEM_ACTOR = "SYSTEM";

    private final LoadSalesOrderPort loadSalesOrderPort;
    private final SaveSalesOrderPort saveSalesOrderPort;
    private final AppendSalesOrderStatusHistoryPort appendHistoryPort;

    /**
     * 실패 응답 수신 시 비즈니스 상태를 되돌리고 saga를 FAILED로 종료한다.
     *
     * 흐름:
     * 1) soCode로 발주를 조회한다.
     * 2) 이미 보상 완료(FAILED)면 무시한다(멱등).
     * 3) 단계별 보상 — OUTBOUND: APPROVED→REQUESTED, INBOUND: DELIVERED→APPROVED. saga FAILED.
     * 4) 되돌린 상태를 이력으로 기록하고 실패 사유를 WARN 로깅한다.
     *
     * 트랜잭션: 쓰기(독립). reply listener가 비동기로 호출한다.
     * 상태 전환·저장·이력이 한 트랜잭션으로 묶인다.
     */
    @Override
    @Transactional
    public void compensate(CompensateStockSagaCommand command) {
        SalesOrder order = loadSalesOrderPort.load(command.soCode());
        // FAILED(이미 보상)·DONE(이미 성공) 모두 terminal — 늦게 온 거절로 완료 주문을 되돌리지 않는다.
        SagaStatus saga = order.getSagaStatus();
        if (saga == SagaStatus.FAILED || saga == SagaStatus.DONE) {
            return;
        }

        switch (command.stage()) {
            case OUTBOUND -> order.compensateApprove();
            case INBOUND -> order.compensateDeliver();
        }
        saveSalesOrderPort.save(order);

        appendHistoryPort.append(SalesOrderStatusHistory.of(
                order.getCode(), order.getStatus(), ActorRef.codeOnly(SYSTEM_ACTOR), Instant.now()));

        log.warn("재고 saga 보상 수행 soCode={} stage={} status={} reason={}",
                command.soCode(), command.stage(), order.getStatus(), command.reason());
    }
}
