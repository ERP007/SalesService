package com.fallguys.salesservice.domain.exception;

public class InvalidStatusTransitionException extends BusinessException {

    public InvalidStatusTransitionException(SalesErrorCode errorCode, String message) {
        super(errorCode.getCode(), message);
    }
}
