package com.fallguys.salesservice.domain.exception;

import lombok.Getter;

@Getter
public class ExternalServiceException extends RuntimeException {

    private final String code;

    public ExternalServiceException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
