package com.fallguys.salesservice.domain.exception;

public class ForbiddenException extends RuntimeException {

    private final String code;

    public ForbiddenException(SalesErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.code = errorCode.getCode();
    }

    public String getCode() {
        return code;
    }
}
