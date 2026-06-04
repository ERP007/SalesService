package com.fallguys.salesservice.domain.exception;

public class SalesOrderException extends BusinessException {

    public SalesOrderException(SalesErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getDefaultMessage());
    }

    public SalesOrderException(SalesErrorCode errorCode, String message) {
        super(errorCode.getCode(), message);
    }
}
