package com.fallguys.salesservice.adapter.inbound.event;

import com.fallguys.salesservice.adapter.inbound.event.dto.StockReplyMessage;
import com.fallguys.salesservice.application.port.inbound.command.CompensateStockSagaCommand;
import com.fallguys.salesservice.application.port.inbound.command.StockSagaStage;
import com.fallguys.salesservice.application.port.inbound.usecase.CompensateStockSagaUseCase;
import com.fallguys.salesservice.application.port.inbound.usecase.CompleteStockSagaUseCase;
import com.fallguys.salesservice.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 재고 서비스 응답(erp.event → sales.inventory.reply 큐)을 수신해 saga를 마무리한다.
 * applied → DONE, rejected → 보상. routing key로 단계(outbound/inbound)를 구분한다.
 *
 * 멱등성은 use case가 saga 상태로 보장하므로 리스너는 라우팅·파싱만 담당한다.
 * ALREADY_PROCESSED 거절은 재고 측 멱등 응답이므로 성공(applied)으로 취급한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockReplyEventListener {

    private static final String ALREADY_PROCESSED = "ALREADY_PROCESSED";

    private final ObjectMapper objectMapper;
    private final CompleteStockSagaUseCase completeStockSagaUseCase;
    private final CompensateStockSagaUseCase compensateStockSagaUseCase;

    @RabbitListener(queues = RabbitConfig.REPLY_QUEUE)
    public void onReply(Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        StockReplyMessage reply = objectMapper.readValue(message.getBody(), StockReplyMessage.class);
        String soCode = reply.correlationId();

        switch (routingKey) {
            case RabbitConfig.RK_OUTBOUND_APPLIED, RabbitConfig.RK_INBOUND_APPLIED ->
                    completeStockSagaUseCase.complete(soCode);
            case RabbitConfig.RK_OUTBOUND_REJECTED ->
                    handleRejected(soCode, StockSagaStage.OUTBOUND, reply);
            case RabbitConfig.RK_INBOUND_REJECTED ->
                    handleRejected(soCode, StockSagaStage.INBOUND, reply);
            default -> log.warn("알 수 없는 reply routing key={} soCode={}", routingKey, soCode);
        }
    }

    private void handleRejected(String soCode, StockSagaStage stage, StockReplyMessage reply) {
        // 이미 처리됨(멱등 응답)은 성공으로 간주 — 보상하면 정상 처리분을 되돌리게 된다.
        if (ALREADY_PROCESSED.equals(reply.errorCode())) {
            completeStockSagaUseCase.complete(soCode);
            return;
        }
        String reason = reply.errorCode() + ": " + reply.errorMessage();
        compensateStockSagaUseCase.compensate(new CompensateStockSagaCommand(soCode, stage, reason));
    }
}
