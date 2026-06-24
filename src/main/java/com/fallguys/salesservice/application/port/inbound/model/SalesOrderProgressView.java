package com.fallguys.salesservice.application.port.inbound.model;

import com.fallguys.salesservice.domain.model.salesorder.OrderProgress;
import com.fallguys.salesservice.domain.model.salesorder.ProgressOutcome;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;

/**
 * 진행 페이지 폴링 응답 모델. pending·outcome은 백엔드가 saga 상태로 판단(조합 규칙 미노출).
 */
public record SalesOrderProgressView(
        String code,
        OrderProgress progress,
        boolean pending,
        ProgressOutcome outcome,
        String failureReason
) {
    public static SalesOrderProgressView from(SalesOrder order) {
        ProgressOutcome outcome = order.sagaOutcome();
        String reason = outcome == ProgressOutcome.FAILED ? order.getLastFailureReason() : null;
        return new SalesOrderProgressView(
                order.getCode(),
                order.progress(),
                outcome == ProgressOutcome.PENDING,
                outcome,
                reason
        );
    }
}
