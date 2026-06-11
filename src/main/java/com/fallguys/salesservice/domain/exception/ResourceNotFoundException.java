package com.fallguys.salesservice.domain.exception;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getDefaultMessage());
    }

    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode.getCode(), message);
    }
}
