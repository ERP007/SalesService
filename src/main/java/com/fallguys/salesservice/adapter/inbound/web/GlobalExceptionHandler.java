package com.fallguys.salesservice.adapter.inbound.web;

import com.fallguys.salesservice.domain.exception.BusinessException;
import com.fallguys.salesservice.domain.exception.CommonErrorCode;
import com.fallguys.salesservice.domain.exception.ExternalServiceException;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.InvalidStatusTransitionException;
import com.fallguys.salesservice.domain.exception.ResourceNotFoundException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException ex) {
        if (ex.getCause() != null) {
            // 외부 서비스 에러를 번역한 경우 원본 원인(상태·응답 본문)을 함께 남긴다.
            log.warn("Business exception: code={}, message={}, cause={}",
                    ex.getCode(), ex.getMessage(), ex.getCause().toString(), ex.getCause());
        } else {
            log.warn("Business exception: code={}, message={}", ex.getCode(), ex.getMessage());
        }
        return build(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: code={}, message={}", ex.getCode(), ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ProblemDetail handleForbidden(ForbiddenException ex) {
        log.warn("Forbidden: code={}, message={}", ex.getCode(), ex.getMessage());
        return build(HttpStatus.FORBIDDEN, ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ProblemDetail handleConflict(InvalidStatusTransitionException ex) {
        log.warn("Invalid status transition: code={}, message={}", ex.getCode(), ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
        log.warn("Optimistic lock conflict: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, SalesErrorCode.CONCURRENT_MODIFICATION.getCode(),
                SalesErrorCode.CONCURRENT_MODIFICATION.getDefaultMessage());
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ProblemDetail handleExternalService(ExternalServiceException ex) {
        log.error("External service error: code={}, message={}", ex.getCode(), ex.getMessage(), ex);
        return build(HttpStatus.BAD_GATEWAY, ex.getCode(), "일시적으로 서비스를 이용할 수 없습니다");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                CommonErrorCode.SERVER_ERROR.getCode(),
                CommonErrorCode.SERVER_ERROR.getDefaultMessage());
    }

    @Override
    protected org.springframework.http.ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            org.springframework.http.@NonNull HttpHeaders headers,
            org.springframework.http.@NonNull HttpStatusCode status,
            org.springframework.web.context.request.@NonNull WebRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        ProblemDetail pd = build(HttpStatus.BAD_REQUEST, SalesErrorCode.INVALID_REQUEST.getCode(), detail);
        return org.springframework.http.ResponseEntity.badRequest().body(pd);
    }

    private ProblemDetail build(HttpStatus status, String errorCode, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setProperty("errorCode", errorCode);
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }
}
