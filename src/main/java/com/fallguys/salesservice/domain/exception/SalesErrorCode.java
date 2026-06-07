package com.fallguys.salesservice.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SalesErrorCode {
    DUPLICATE_ITEM("SO-05-01", "동일한 부품이 중복 포함되어 있습니다"),
    INVALID_DESIRED_ARRIVAL_DATE("SO-05-02", "도착 희망일이 유효하지 않습니다"),
    UNAUTHORIZED("SO-05-03", "해당 작업에 대한 권한이 없습니다"),
    WAREHOUSE_NOT_FOUND("SO-05-04", "존재하지 않는 창고입니다"),
    ITEM_NOT_FOUND("SO-05-05", "존재하지 않는 부품입니다"),
    USER_NOT_FOUND("SO-05-06", "존재하지 않는 사용자입니다"),
    INVALID_STATUS_TRANSITION("SO-05-07", "현재 상태에서 허용되지 않는 작업입니다"),
    SO_NOT_FOUND("SO-06-01", "존재하지 않는 발주입니다"),
    SO_FORBIDDEN("SO-06-02", "해당 발주에 대한 접근 권한이 없습니다"),
    INVALID_QUERY_PARAM("SO-05-08", "유효하지 않은 조회 파라미터입니다"),
    INVENTORY_INBOUND_FAILED("SO-07-01", "재고 입고 처리에 실패했습니다"),
    INVENTORY_ALREADY_PROCESSED("SO-07-02", "이미 입고 처리된 발주입니다"),
    INVENTORY_WAREHOUSE_INACTIVE("SO-07-03", "비활성 창고로는 입고할 수 없습니다"),
    INVENTORY_SERVICE_ERROR("SO-07-04", "재고 서비스 호출에 실패했습니다");

    private final String code;
    private final String defaultMessage;
}
