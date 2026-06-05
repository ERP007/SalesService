package com.fallguys.salesservice.domain.exception;

public class ConflictException extends BusinessException {

    public ConflictException(SalesErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getDefaultMessage());
    }

    public ConflictException(SalesErrorCode errorCode, String message) {
        super(errorCode.getCode(), message);
    }
}
