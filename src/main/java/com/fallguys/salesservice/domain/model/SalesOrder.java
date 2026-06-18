package com.fallguys.salesservice.domain.model;

import com.fallguys.salesservice.domain.exception.InvalidStatusTransitionException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class SalesOrder {

    private final String code;
    private final String fromWarehouseCode;
    private String toWarehouseCode;
    private SalesOrderStatus status;
    private LocalDate desiredArrivalDate;
    private String requestMemo;

    private final SalesOrderCreation creation;
    private SalesOrderRequest request;
    private SalesOrderApproval approval;
    private SalesOrderRejection rejection;
    private SalesOrderDelivery delivery;
    private SalesOrderCancellation cancellation;

    private List<SalesOrderLine> lines;

    /**
     * DRAFT 상태의 발주를 DRAFT 그대로 수정한다.
     *
     * 흐름:
     * 1) DRAFT 상태인지 검증한다.
     * 2) 창고·날짜·메모·라인을 덮어쓴다. 상태·이력은 변경하지 않는다.
     *
     * 예외:
     * - DRAFT가 아닌 경우: InvalidStatusTransitionException (SO-018, 409)
     */
    public void updateDraft(String toWarehouseCode, LocalDate desiredArrivalDate,
                            String requestMemo, List<SalesOrderLine> lines) {
        if (this.status != SalesOrderStatus.DRAFT) {
            throw new InvalidStatusTransitionException(SalesErrorCode.INVALID_STATUS_TRANSITION,
                    "DRAFT 상태에서만 수정 가능합니다. 현재 상태: " + this.status);
        }
        this.toWarehouseCode = toWarehouseCode;
        this.desiredArrivalDate = desiredArrivalDate;
        this.requestMemo = requestMemo;
        this.lines = lines;
    }

    /**
     * DRAFT 상태의 발주를 REQUESTED로 전환한다.
     *
     * 흐름:
     * 1) DRAFT 상태인지 검증한다.
     * 2) 요청 데이터(창고, 날짜, 메모, 라인)를 덮어쓴다.
     * 3) 상태를 REQUESTED로, request 이력을 기록한다.
     *
     * 예외:
     * - DRAFT가 아닌 경우: InvalidStatusTransitionException (SO-018, 409)
     */
    public void submitRequest(String requestedBy, Instant now, String toWarehouseCode,
                              LocalDate desiredArrivalDate, String requestMemo,
                              List<SalesOrderLine> lines) {
        if (this.status != SalesOrderStatus.DRAFT) {
            throw new InvalidStatusTransitionException(SalesErrorCode.INVALID_STATUS_TRANSITION,
                    "DRAFT 상태에서만 요청 가능합니다. 현재 상태: " + this.status);
        }
        this.toWarehouseCode = toWarehouseCode;
        this.desiredArrivalDate = desiredArrivalDate;
        this.requestMemo = requestMemo;
        this.lines = lines;
        this.status = SalesOrderStatus.REQUESTED;
        this.request = new SalesOrderRequest(requestedBy, now);
    }

    /**
     * REQUESTED 상태의 발주를 APPROVED로 전환한다.
     *
     * 흐름:
     * 1) REQUESTED 상태인지 검증한다.
     * 2) 각 라인의 approvedQuantity를 requestedQuantity로 확정한다.
     * 3) 승인 이력(approvedBy, approvedAt, approvedDate, carrierType, invoiceNumber)을 기록하고 상태를 APPROVED로 전환한다.
     *
     * 트랜잭션: 쓰기. 조회·승인·저장이 한 트랜잭션으로 묶이며 예외 시 전체 롤백.
     *
     * 예외:
     * - REQUESTED가 아닌 경우: InvalidStatusTransitionException (SO-018, 409)
     */
    public void approve(String approvedBy, Instant now, LocalDate approvedDate,
                        CarrierType carrierType, String invoiceNumber) {
        if (this.status != SalesOrderStatus.REQUESTED) {
            throw new InvalidStatusTransitionException(SalesErrorCode.INVALID_STATUS_TRANSITION,
                    "REQUESTED 상태에서만 승인 가능합니다. 현재 상태: " + this.status);
        }
        this.lines.forEach(SalesOrderLine::approve);
        this.approval = new SalesOrderApproval(approvedBy, now, approvedDate, carrierType, invoiceNumber);
        this.status = SalesOrderStatus.APPROVED;
    }

    /**
     * APPROVED 상태의 발주를 DELIVERED로 전환한다.
     *
     * 흐름:
     * 1) APPROVED 상태인지 검증한다.
     * 2) 각 라인의 deliveredQuantity를 approvedQuantity로 확정한다(이번 단계: 차이 없음).
     * 3) 배송 이력(deliveredBy, deliveredAt)을 기록하고 상태를 DELIVERED로 전환한다.
     *
     * 예외:
     * - APPROVED가 아닌 경우: InvalidStatusTransitionException (SO-018, 409)
     */
    public void deliver(String deliveredBy, LocalDate deliveredDate, Instant now) {
        if (this.status != SalesOrderStatus.APPROVED) {
            throw new InvalidStatusTransitionException(SalesErrorCode.INVALID_STATUS_TRANSITION,
                    "APPROVED 상태에서만 배송 처리 가능합니다. 현재 상태: " + this.status);
        }
        this.lines.forEach(SalesOrderLine::confirmDelivery);
        this.delivery = new SalesOrderDelivery(deliveredBy, deliveredDate, now, null, null);
        this.status = SalesOrderStatus.DELIVERED;
    }

    /**
     * REQUESTED 상태의 발주를 REJECTED로 전환한다.
     *
     * 흐름:
     * 1) REQUESTED 상태인지 검증한다.
     * 2) 반려 이력(rejectedBy, rejectedAt, category, memo)을 기록하고 상태를 REJECTED로 전환한다.
     *
     * 트랜잭션: 쓰기. 조회·반려·저장이 한 트랜잭션으로 묶이며 예외 시 전체 롤백.
     *
     * 예외:
     * - REQUESTED가 아닌 경우: InvalidStatusTransitionException (SO-018, 409)
     */
    public void reject(String rejectedBy, Instant now, RejectReasonCategory reasonCategory, String memo) {
        if (this.status != SalesOrderStatus.REQUESTED) {
            throw new InvalidStatusTransitionException(SalesErrorCode.INVALID_STATUS_TRANSITION,
                    "REQUESTED 상태에서만 반려 가능합니다. 현재 상태: " + this.status);
        }
        this.rejection = new SalesOrderRejection(rejectedBy, now, reasonCategory, memo);
        this.status = SalesOrderStatus.REJECTED;
    }

    /**
     * REQUESTED 상태의 발주를 CANCELED로 전환한다.
     *
     * 흐름:
     * 1) REQUESTED 상태인지 검증한다.
     * 2) 취소 이력(canceledBy, canceledAt, reason)을 기록한다.
     * 3) 상태를 CANCELED로 전환한다.
     *
     * 트랜잭션: 쓰기. 조회·취소·저장이 한 트랜잭션으로 묶이며 예외 시 전체 롤백.
     *
     * 예외:
     * - REQUESTED가 아닌 경우: InvalidStatusTransitionException (SO-018, 409)
     */
    public void cancel(String canceledBy, Instant now, String reason) {
        if (this.status != SalesOrderStatus.REQUESTED) {
            throw new InvalidStatusTransitionException(SalesErrorCode.INVALID_STATUS_TRANSITION,
                    "REQUESTED 상태에서만 취소 가능합니다. 현재 상태: " + this.status);
        }
        this.cancellation = new SalesOrderCancellation(canceledBy, now, reason);
        this.status = SalesOrderStatus.CANCELED;
    }
}
