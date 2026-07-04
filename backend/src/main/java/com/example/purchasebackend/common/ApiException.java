package com.example.purchasebackend.common;

import java.util.Map;

/**
 * The exception every business-logic failure throws. Carries a stable {@link ErrorCode} (plus an
 * optional override message and details), which {@link GlobalExceptionHandler} turns into a
 * consistent {@link ApiResponse} error with the right HTTP status.
 */
public class ApiException extends RuntimeException {

    private final transient ErrorCode errorCode;
    private final transient Map<String, Object> details;

    public ApiException(ErrorCode errorCode) {
        this(errorCode, errorCode.defaultMessage(), null);
    }

    public ApiException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public ApiException(ErrorCode errorCode, String message, Map<String, Object> details) {
        super(message != null ? message : errorCode.defaultMessage());
        this.errorCode = errorCode;
        this.details = details;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public Map<String, Object> details() {
        return details;
    }
}
