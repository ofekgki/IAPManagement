package com.example.purchasebackend.common;

import com.example.purchasebackend.service.ErrorMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Turns every thrown exception into the consistent {@link ApiResponse} error envelope with the
 * correct HTTP status. Business failures use {@link ApiException}; validation failures map to
 * {@code INVALID_REQUEST}; anything unexpected becomes {@code INTERNAL_ERROR} (and is logged).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ErrorMapper errorMapper;

    public GlobalExceptionHandler(ErrorMapper errorMapper) {
        this.errorMapper = errorMapper;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Object>> handleApi(ApiException ex) {
        ApiError error = errorMapper.toApiError(ex);
        return ResponseEntity.status(ex.errorCode().httpStatus())
                .body(ApiResponse.fail(error, RequestContext.getRequestId()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> details = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> details.put(fe.getField(), fe.getDefaultMessage()));
        ApiError error = ApiError.of(ErrorCode.INVALID_REQUEST, "One or more fields are invalid.", details);
        return ResponseEntity.status(ErrorCode.INVALID_REQUEST.httpStatus())
                .body(ApiResponse.fail(error, RequestContext.getRequestId()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        ApiError error = ApiError.of(ErrorCode.INTERNAL_ERROR, null, null);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.httpStatus())
                .body(ApiResponse.fail(error, RequestContext.getRequestId()));
    }
}
