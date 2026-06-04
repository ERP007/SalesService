package com.fallguys.salesservice.domain.exception;

public class ForbiddenException extends BusinessException {

    public ForbiddenException(SalesErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getDefaultMessage());
    }
}
