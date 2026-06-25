package com.fallguys.salesservice.adapter.outbound.messaging;

import com.fallguys.salesservice.adapter.outbound.messaging.event.BaseEvent;
import com.fallguys.salesservice.adapter.outbound.messaging.event.UserActivityPayload;
import com.fallguys.salesservice.application.port.outbound.model.UserActivity;
import com.fallguys.salesservice.application.port.outbound.port.PublishUserActivityPort;
import com.fallguys.salesservice.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

/**
 * 사용자 활동을 erp.events(routing key user.activity.occurred)로 비동기 발행한다.
 * stock saga(outbox+confirm)와 달리 정합성 보장 없는 best-effort 감사 발행이라,
 * 발행 실패는 WARN 로깅만 하고 삼킨다(호출자 트랜잭션·흐름에 영향 없음).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserActivityMessagingAdapter implements PublishUserActivityPort {

    private static final String EVENT_TYPE = "user.activity.occurred";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Async("outboxRelayExecutor")
    public void publish(UserActivity activity) {
        try {
            // correlationId는 saga 키가 아니라 활동 대상 식별용으로 title(발주번호)을 사용한다.
            BaseEvent<UserActivityPayload> event = BaseEvent.of(
                    EVENT_TYPE, activity.title(), UserActivityPayload.from(activity), activity.occurredAt());

            var message = MessageBuilder
                    .withBody(objectMapper.writeValueAsString(event).getBytes(StandardCharsets.UTF_8))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setContentEncoding(StandardCharsets.UTF_8.name())
                    .setMessageId(event.eventId().toString())
                    .build();

            rabbitTemplate.send(RabbitConfig.EVENT_EXCHANGE, RabbitConfig.RK_USER_ACTIVITY, message);
        } catch (Exception e) {
            log.warn("사용자 활동 발행 실패(무시): employeeNo={}, action={}, title={}",
                    activity.employeeNo(), activity.action(), activity.title(), e);
        }
    }
}
