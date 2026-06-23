package com.fallguys.salesservice.adapter.inbound.event.dto;

/**
 * 재고 서비스가 erp.event로 보내는 응답(applied/rejected) 수신 DTO.
 * BaseEvent envelope 중 필요한 필드만 매핑한다(나머지는 역직렬화 시 무시).
 *
 * applied: payload.status=SUCCEEDED, movements 등은 사용하지 않음.
 * rejected: payload.status=FAILED, errorCode/errorMessage.
 */
public record StockReplyMessage(
        String correlationId,
        ReplyPayload payload
) {
    public record ReplyPayload(
            String sourceRef,
            String warehouseCode,
            String status,
            String errorCode,
            String errorMessage
    ) {
    }

    public String errorCode() {
        return payload != null ? payload.errorCode() : null;
    }

    public String errorMessage() {
        return payload != null ? payload.errorMessage() : null;
    }
}
