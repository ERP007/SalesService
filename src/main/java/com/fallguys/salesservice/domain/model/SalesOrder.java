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
     * DRAFT 상태의 발주를 REQUESTED로 전환한다.
     *
     * 흐름:
     * 1) DRAFT 상태인지 검증한다.
     * 2) 요청 데이터(창고, 날짜, 메모, 라인)를 덮어쓴다.
     * 3) 상태를 REQUESTED로, request 이력을 기록한다.
     *
     * 예외:
     * - DRAFT가 아닌 경우: SalesOrderException (SO-05-07, 400)
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
     * - REQUESTED가 아닌 경우: SalesOrderException (SO-05-07, 400)
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
