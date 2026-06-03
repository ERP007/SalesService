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
    ITEM_NOT_FOUND("SO-05-05", "존재하지 않는 부품입니다");

    private final String code;
    private final String defaultMessage;
}
