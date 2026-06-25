package com.fallguys.salesservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 토폴로지.
 * - 발신: erp.commands(topic), routing key = 이벤트 wire string
 * - 수신: erp.event(topic) → sales 소유 reply 큐로 성공/실패 응답 바인딩
 */
@Configuration
public class RabbitConfig {

    public static final String COMMANDS_EXCHANGE = "erp.commands";
    public static final String EVENT_EXCHANGE = "erp.events";
    public static final String REPLY_QUEUE = "sales.inventory.reply";

    // 재시도 소진·처리 불가 메시지를 격리하는 데드레터
    public static final String REPLY_DLX = "sales.inventory.reply.dlx";
    public static final String REPLY_DLQ = "sales.inventory.reply.dlq";

    // user-service로 보내는 사용자 활동 이력 routing key (erp.events 발행)
    public static final String RK_USER_ACTIVITY = "user.activity.occurred";

    // inventory가 erp.event로 발행하는 응답 routing key
    public static final String RK_OUTBOUND_APPLIED = "inventory.stock.outbound.applied.sales";
    public static final String RK_OUTBOUND_REJECTED = "inventory.stock.outbound.rejected.sales";
    public static final String RK_INBOUND_APPLIED = "inventory.stock.inbound.applied.sales";
    public static final String RK_INBOUND_REJECTED = "inventory.stock.inbound.rejected.sales";

    @Bean
    TopicExchange commandsExchange() {
        return new TopicExchange(COMMANDS_EXCHANGE, true, false);
    }

    @Bean
    TopicExchange eventExchange() {
        return new TopicExchange(EVENT_EXCHANGE, true, false);
    }

    @Bean
    Queue replyQueue() {
        // 처리 실패(재시도 소진) 시 메시지를 DLX로 보낸다.
        return QueueBuilder.durable(REPLY_QUEUE)
                .deadLetterExchange(REPLY_DLX)
                .build();
    }

    @Bean
    FanoutExchange replyDlx() {
        return new FanoutExchange(REPLY_DLX, true, false);
    }

    @Bean
    Queue replyDlq() {
        return QueueBuilder.durable(REPLY_DLQ).build();
    }

    @Bean
    Binding bindReplyDlq(Queue replyDlq, FanoutExchange replyDlx) {
        return BindingBuilder.bind(replyDlq).to(replyDlx);
    }

    @Bean
    Binding bindOutboundApplied(Queue replyQueue, TopicExchange eventExchange) {
        return BindingBuilder.bind(replyQueue).to(eventExchange).with(RK_OUTBOUND_APPLIED);
    }

    @Bean
    Binding bindOutboundRejected(Queue replyQueue, TopicExchange eventExchange) {
        return BindingBuilder.bind(replyQueue).to(eventExchange).with(RK_OUTBOUND_REJECTED);
    }

    @Bean
    Binding bindInboundApplied(Queue replyQueue, TopicExchange eventExchange) {
        return BindingBuilder.bind(replyQueue).to(eventExchange).with(RK_INBOUND_APPLIED);
    }

    @Bean
    Binding bindInboundRejected(Queue replyQueue, TopicExchange eventExchange) {
        return BindingBuilder.bind(replyQueue).to(eventExchange).with(RK_INBOUND_REJECTED);
    }
}
