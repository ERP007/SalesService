package com.fallguys.salesservice.adapter.outbound.messaging.event;

import com.fallguys.salesservice.application.port.outbound.model.UserActivity;

/**
 * user.activity.occurred payload(wire 포맷). occurredAt은 ISO-8601 문자열.
 */
public record UserActivityPayload(
        String employeeNo,
        String action,
        String occurredAt,
        String title,
        String content,
        String status
) {
    public static UserActivityPayload from(UserActivity activity) {
        return new UserActivityPayload(
                activity.employeeNo(),
                activity.action().name(),
                activity.occurredAt().toString(),
                activity.title(),
                activity.content(),
                activity.status());
    }
}
