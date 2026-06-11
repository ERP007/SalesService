package com.fallguys.salesservice.domain.exception;

public class InvalidStatusTransitionException extends BusinessException {

    public InvalidStatusTransitionException(ErrorCode errorCode, String message) {
        super(errorCode.getCode(), message);
    }
}
