package com.fallguys.salesservice.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {
    UNAUTHORIZED("ER-403", "해당 작업에 대한 권한이 없습니다"),
    SERVER_ERROR("ER-500", "서버 오류가 발생했습니다"),
    EXTERNAL_SERVICE_ERROR("ER-502", "일시적으로 서비스를 이용할 수 없습니다");

    private final String code;
    private final String defaultMessage;
}
