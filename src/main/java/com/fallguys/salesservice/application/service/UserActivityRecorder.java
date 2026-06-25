package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.outbound.model.UserActivity;
import com.fallguys.salesservice.application.port.outbound.model.UserActivityAction;
import com.fallguys.salesservice.application.port.outbound.port.PublishUserActivityPort;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 발주 유스케이스의 확정 지점에서 사용자 활동을 User 서비스로 발행하는 헬퍼.
 * title/content/status(화면 표시 문구) 매핑을 한곳에 모은다. 발행 자체는 best-effort(비동기·실패 무시).
 */
@Component
@RequiredArgsConstructor
public class UserActivityRecorder {

    private final PublishUserActivityPort publishUserActivityPort;

    public void created(SalesOrder order, String employeeNo, Instant occurredAt) {
        publishUserActivityPort.publish(new UserActivity(
                employeeNo, UserActivityAction.SALES_ORDER_CREATED, occurredAt,
                order.getCode(), lineContent(order), statusLabel(order.getStatus())));
    }

    public void updated(SalesOrder order, String employeeNo, Instant occurredAt) {
        publishUserActivityPort.publish(new UserActivity(
                employeeNo, UserActivityAction.SALES_ORDER_UPDATED, occurredAt,
                order.getCode(), lineContent(order), null));
    }

    public void statusChanged(String soCode, SalesOrderStatus status, String employeeNo, Instant occurredAt) {
        publishUserActivityPort.publish(new UserActivity(
                employeeNo, UserActivityAction.SALES_ORDER_STATUS_CHANGED, occurredAt,
                soCode, null, statusLabel(status)));
    }

    private String lineContent(SalesOrder order) {
        int count = order.getLines() == null ? 0 : order.getLines().size();
        return "발주 라인 " + count + "건";
    }

    private String statusLabel(SalesOrderStatus status) {
        return switch (status) {
            case DRAFT -> "임시저장";
            case REQUESTED -> "출고대기";
            case APPROVED -> "출고";
            case REJECTED -> "거절";
            case CANCELED -> "취소";
            case DELIVERED -> "입고";
        };
    }
}
