package com.fallguys.salesservice.domain.model.salesorder;

import com.fallguys.salesservice.domain.exception.InvalidStatusTransitionException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.model.salesorderline.SalesOrderLine;
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
    private SagaStatus sagaStatus;
    private LocalDate desiredArrivalDate;
    private String requestMemo;

    private final SalesOrderCreation creation;
    private SalesOrderRequest request;

    private List<SalesOrderLine> lines;

    /**
     * 기존 9-arg 호출부(생성·테스트) 호환용. sagaStatus는 NONE으로 초기화한다.
     * 영속성 복원(toDomain)은 sagaStatus를 포함한 @AllArgsConstructor 10-arg를 사용한다.
     */
    public SalesOrder(String code, String fromWarehouseCode, String toWarehouseCode,
                      SalesOrderStatus status, LocalDate desiredArrivalDate, String requestMemo,
                      SalesOrderCreation creation, SalesOrderRequest request, List<SalesOrderLine> lines) {
        this(code, fromWarehouseCode, toWarehouseCode, status, SagaStatus.NONE,
                desiredArrivalDate, requestMemo, creation, request, lines);
    }

    /**
     * 발주를 신규 생성한다.
     *
     * 흐름:
     * 1) 생성 이력(createdBy, now)을 항상 기록한다.
     * 2) 즉시 요청(REQUESTED)이면 요청 이력(requestedBy=createdBy, now)도 기록하고, DRAFT면 남기지 않는다.
     *
     * 상태별 분기(REQUESTED → request 기록)는 도메인 규칙이므로 여기서 결정한다.
     */
    public static SalesOrder create(String code, String fromWarehouseCode, String toWarehouseCode,
                                    SalesOrderStatus status, LocalDate desiredArrivalDate, String requestMemo,
                                    String createdBy, Instant now, List<SalesOrderLine> lines) {
        SalesOrderRequest request = status == SalesOrderStatus.REQUESTED
                ? new SalesOrderRequest(createdBy, now)
                : null;
        return new SalesOrder(
                code, fromWarehouseCode, toWarehouseCode, status, desiredArrivalDate, requestMemo,
                new SalesOrderCreation(createdBy, now),
                request,
                lines
        );
    }

    /**
     * DRAFT 상태의 발주를 DRAFT 그대로 수정한다.
     *
     * 흐름:
     * 1) DRAFT 상태인지 검증한다.
     * 2) 창고·날짜·메모·라인을 덮어쓴다. 상태는 변경하지 않는다.
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
     * 3) 상태를 REQUESTED로, request(요청자·요청시각) 운영 정보를 기록한다.
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
     * 2) 상태를 APPROVED로 전환한다. 승인 부가 데이터는 상태 변경 이력으로 별도 기록한다.
     *
     * 예외:
     * - REQUESTED가 아닌 경우: InvalidStatusTransitionException (SO-018, 409)
     */
    public void approve() {
        if (this.status != SalesOrderStatus.REQUESTED) {
            throw new InvalidStatusTransitionException(SalesErrorCode.INVALID_STATUS_TRANSITION,
                    "REQUESTED 상태에서만 승인 가능합니다. 현재 상태: " + this.status);
        }
        this.status = SalesOrderStatus.APPROVED;
        this.sagaStatus = SagaStatus.SENDING;
    }

    /**
     * APPROVED 상태의 발주를 DELIVERED로 전환한다.
     *
     * 흐름:
     * 1) APPROVED 상태인지 검증한다.
     * 2) 상태를 DELIVERED로 전환한다. 배송 부가 데이터는 상태 변경 이력으로 별도 기록한다.
     *
     * 예외:
     * - APPROVED가 아닌 경우: InvalidStatusTransitionException (SO-018, 409)
     */
    public void deliver() {
        if (this.status != SalesOrderStatus.APPROVED) {
            throw new InvalidStatusTransitionException(SalesErrorCode.INVALID_STATUS_TRANSITION,
                    "APPROVED 상태에서만 배송 처리 가능합니다. 현재 상태: " + this.status);
        }
        this.status = SalesOrderStatus.DELIVERED;
        this.sagaStatus = SagaStatus.SENDING;
    }

    /**
     * REQUESTED 상태의 발주를 REJECTED로 전환한다.
     *
     * 흐름:
     * 1) REQUESTED 상태인지 검증한다.
     * 2) 상태를 REJECTED로 전환한다. 반려 사유는 상태 변경 이력으로 별도 기록한다.
     *
     * 예외:
     * - REQUESTED가 아닌 경우: InvalidStatusTransitionException (SO-018, 409)
     */
    public void reject() {
        if (this.status != SalesOrderStatus.REQUESTED) {
            throw new InvalidStatusTransitionException(SalesErrorCode.INVALID_STATUS_TRANSITION,
                    "REQUESTED 상태에서만 반려 가능합니다. 현재 상태: " + this.status);
        }
        this.status = SalesOrderStatus.REJECTED;
    }

    /**
     * REQUESTED 상태의 발주를 CANCELED로 전환한다.
     *
     * 흐름:
     * 1) REQUESTED 상태인지 검증한다.
     * 2) 상태를 CANCELED로 전환한다. 취소 사유는 상태 변경 이력으로 별도 기록한다.
     *
     * 예외:
     * - REQUESTED가 아닌 경우: InvalidStatusTransitionException (SO-018, 409)
     */
    public void cancel() {
        if (this.status != SalesOrderStatus.REQUESTED) {
            throw new InvalidStatusTransitionException(SalesErrorCode.INVALID_STATUS_TRANSITION,
                    "REQUESTED 상태에서만 취소 가능합니다. 현재 상태: " + this.status);
        }
        this.status = SalesOrderStatus.CANCELED;
    }

    /**
     * saga를 SENDING → PROCESSING으로 전환한다(릴레이 발행 성공 시).
     *
     * 예외:
     * - SENDING이 아닌 경우: InvalidStatusTransitionException (SO-018)
     */
    public void markSagaProcessing() {
        if (this.sagaStatus != SagaStatus.SENDING) {
            throw new InvalidStatusTransitionException(SalesErrorCode.INVALID_STATUS_TRANSITION,
                    "SENDING 상태에서만 PROCESSING 전환 가능합니다. 현재 saga 상태: " + this.sagaStatus);
        }
        this.sagaStatus = SagaStatus.PROCESSING;
    }

    /**
     * saga를 PROCESSING → DONE으로 전환한다(재고 서비스 성공 응답 수신 시).
     *
     * 예외:
     * - PROCESSING이 아닌 경우: InvalidStatusTransitionException (SO-018)
     */
    public void markSagaDone() {
        if (this.sagaStatus != SagaStatus.PROCESSING) {
            throw new InvalidStatusTransitionException(SalesErrorCode.INVALID_STATUS_TRANSITION,
                    "PROCESSING 상태에서만 DONE 전환 가능합니다. 현재 saga 상태: " + this.sagaStatus);
        }
        this.sagaStatus = SagaStatus.DONE;
    }

    /**
     * 출고 saga 실패 보상: APPROVED를 REQUESTED로 되돌리고 saga를 FAILED로 종료한다.
     *
     * 흐름:
     * 1) APPROVED 상태인지 검증한다.
     * 2) 상태를 REQUESTED로 되돌린다(재승인·반려 가능).
     * 3) saga를 FAILED로 종료한다.
     *
     * 예외:
     * - APPROVED가 아닌 경우: InvalidStatusTransitionException (SO-018)
     */
    public void compensateApprove() {
        if (this.status != SalesOrderStatus.APPROVED) {
            throw new InvalidStatusTransitionException(SalesErrorCode.INVALID_STATUS_TRANSITION,
                    "APPROVED 상태에서만 출고 보상 가능합니다. 현재 상태: " + this.status);
        }
        this.status = SalesOrderStatus.REQUESTED;
        this.sagaStatus = SagaStatus.FAILED;
    }

    /**
     * 입고 saga 실패 보상: DELIVERED를 APPROVED로 되돌리고 saga를 FAILED로 종료한다.
     *
     * 흐름:
     * 1) DELIVERED 상태인지 검증한다.
     * 2) 상태를 APPROVED로 되돌린다(재배송 처리 가능).
     * 3) saga를 FAILED로 종료한다.
     *
     * 예외:
     * - DELIVERED가 아닌 경우: InvalidStatusTransitionException (SO-018)
     */
    public void compensateDeliver() {
        if (this.status != SalesOrderStatus.DELIVERED) {
            throw new InvalidStatusTransitionException(SalesErrorCode.INVALID_STATUS_TRANSITION,
                    "DELIVERED 상태에서만 입고 보상 가능합니다. 현재 상태: " + this.status);
        }
        this.status = SalesOrderStatus.APPROVED;
        this.sagaStatus = SagaStatus.FAILED;
    }
}
