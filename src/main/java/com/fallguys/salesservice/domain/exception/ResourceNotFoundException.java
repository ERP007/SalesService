package com.fallguys.salesservice.domain.exception;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(SalesErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getDefaultMessage());
    }

    public ResourceNotFoundException(SalesErrorCode errorCode, String message) {
        super(errorCode.getCode(), message);
    }
}
