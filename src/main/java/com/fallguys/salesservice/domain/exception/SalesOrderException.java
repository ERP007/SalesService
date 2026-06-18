package com.fallguys.salesservice.domain.exception;

public class SalesOrderException extends BusinessException {

    public SalesOrderException(ErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getDefaultMessage());
    }

    public SalesOrderException(ErrorCode errorCode, String message) {
        super(errorCode.getCode(), message);
    }

    public SalesOrderException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getCode(), errorCode.getDefaultMessage(), cause);
    }
}
