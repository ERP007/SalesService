package com.fallguys.salesservice.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SalesErrorCode implements ErrorCode {
    // 400 BAD_REQUEST
    INVALID_REQUEST("SO-001", "요청 본문이 유효하지 않습니다"),
    DUPLICATE_ITEM("SO-002", "동일한 부품이 중복 포함되어 있습니다"),
    INVALID_DESIRED_ARRIVAL_DATE("SO-003", "도착 희망일이 유효하지 않습니다"),
    WAREHOUSE_INACTIVE("SO-004", "비활성화된 창고입니다"),
    ITEM_INACTIVE("SO-005", "비활성화된 부품입니다"),
    DUPLICATE_INVOICE_NUMBER("SO-006", "이미 사용된 송장 번호입니다"),
    INVALID_APPROVED_DATE("SO-007", "승인일은 요청일보다 이전일 수 없습니다"),
    INVALID_DELIVERED_DATE("SO-008", "입고일은 출고 승인일 이전일 수 없습니다"),
    REJECT_MEMO_REQUIRED("SO-009", "기타 사유 선택 시 메모는 필수입니다"),
    INVALID_QUERY_PARAM("SO-010", "유효하지 않은 조회 파라미터입니다"),
    INVENTORY_INBOUND_FAILED("SO-011", "재고 입고 처리에 실패했습니다"),
    INVENTORY_ALREADY_PROCESSED("SO-012", "이미 입고 처리된 발주입니다"),
    INVENTORY_OUTBOUND_FAILED("SO-013", "재고 출고 처리에 실패했습니다"),
    INVENTORY_WAREHOUSE_INACTIVE("SO-014", "비활성 창고로는 처리할 수 없습니다"),
    INSUFFICIENT_STOCK("SO-015", "가용 재고가 부족합니다"),
    INVENTORY_LOCK_TIMEOUT("SO-016", "재고 처리 중 타임아웃이 발생했습니다"),

    // 403 FORBIDDEN
    SO_FORBIDDEN("SO-017", "해당 발주에 대한 접근 권한이 없습니다"),

    // 404 NOT_FOUND
    SO_NOT_FOUND("SO-018", "존재하지 않는 발주입니다"),
    WAREHOUSE_NOT_FOUND("SO-019", "존재하지 않는 창고입니다"),
    ITEM_NOT_FOUND("SO-020", "존재하지 않는 부품입니다"),
    USER_NOT_FOUND("SO-021", "존재하지 않는 사용자입니다"),
    STOCK_NOT_FOUND("SO-022", "존재하지 않는 재고입니다"),

    // 409 CONFLICT
    INVALID_STATUS_TRANSITION("SO-023", "현재 상태에서 허용되지 않는 작업입니다");

    private final String code;
    private final String defaultMessage;
}
