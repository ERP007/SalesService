package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.domain.model.salesorderhistory.ApprovalPayload;
import com.fallguys.salesservice.domain.model.salesorderhistory.CancellationPayload;
import com.fallguys.salesservice.domain.model.salesorderhistory.DeliveryPayload;
import com.fallguys.salesservice.domain.model.salesorderhistory.RejectionPayload;
import com.fallguys.salesservice.domain.model.salesorderhistory.StatusChangePayload;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.LocalDate;

/**
 * 상태 변경 부가 데이터(meta)의 응답 표현. 상태마다 형식이 다르므로 flat nullable이 아니라
 * "type" discriminator를 붙인 다형 JSON으로 직렬화한다(도메인 payload는 무의존이라 매핑한다).
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = StatusChangeMeta.Approval.class, name = "APPROVED"),
        @JsonSubTypes.Type(value = StatusChangeMeta.Rejection.class, name = "REJECTED"),
        @JsonSubTypes.Type(value = StatusChangeMeta.Delivery.class, name = "DELIVERED"),
        @JsonSubTypes.Type(value = StatusChangeMeta.Cancellation.class, name = "CANCELED")
})
public sealed interface StatusChangeMeta {

    record Approval(LocalDate approvedDate, String carrierType, String invoiceNumber) implements StatusChangeMeta {}

    record Rejection(String reasonCategory, String reasonMemo) implements StatusChangeMeta {}

    record Delivery(LocalDate deliveredDate) implements StatusChangeMeta {}

    record Cancellation(String reason) implements StatusChangeMeta {}

    static StatusChangeMeta from(StatusChangePayload payload) {
        return switch (payload) {
            case null -> null;
            case ApprovalPayload p -> new Approval(
                    p.approvedDate(),
                    p.carrierType() != null ? p.carrierType().name() : null,
                    p.invoiceNumber());
            case RejectionPayload p -> new Rejection(
                    p.rejectReasonCategory() != null ? p.rejectReasonCategory().name() : null,
                    p.rejectReasonMemo());
            case DeliveryPayload p -> new Delivery(p.deliveredDate());
            case CancellationPayload p -> new Cancellation(p.cancelReason());
        };
    }
}
