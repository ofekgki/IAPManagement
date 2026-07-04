package com.example.purchasebackend.service;

import com.example.purchasebackend.common.ApiError;
import com.example.purchasebackend.common.ApiException;
import com.example.purchasebackend.common.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * Central mapping of exceptions to the stable {@link ErrorCode} / {@link ApiError} contract. Used by
 * {@code GlobalExceptionHandler} so error shaping lives in one place and stays consistent.
 */
@Component
public class ErrorMapper {

    /** Classifies any throwable into a stable error code. */
    public ErrorCode classify(Throwable throwable) {
        if (throwable instanceof ApiException apiException) {
            return apiException.errorCode();
        }
        return ErrorCode.INTERNAL_ERROR;
    }

    /** Builds the API error payload for a known business exception. */
    public ApiError toApiError(ApiException exception) {
        return ApiError.of(exception.errorCode(), exception.getMessage(), exception.details());
    }
}
