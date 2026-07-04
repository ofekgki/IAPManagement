package com.example.purchasebackend.common;

import java.util.Map;

/** The error payload inside an {@link ApiResponse}. */
public record ApiError(String code, String message, Map<String, Object> details) {

    public static ApiError of(ErrorCode code, String message, Map<String, Object> details) {
        return new ApiError(code.name(), message != null ? message : code.defaultMessage(), details);
    }
}
