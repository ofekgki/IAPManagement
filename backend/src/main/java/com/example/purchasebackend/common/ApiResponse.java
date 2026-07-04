package com.example.purchasebackend.common;

/**
 * The single, consistent response envelope used by every endpoint:
 * <pre>{ "success": true, "data": {...}, "error": null, "requestId": "req_..." }</pre>
 */
public record ApiResponse<T>(boolean success, T data, ApiError error, String requestId) {

    public static <T> ApiResponse<T> ok(T data, String requestId) {
        return new ApiResponse<>(true, data, null, requestId);
    }

    public static <T> ApiResponse<T> fail(ApiError error, String requestId) {
        return new ApiResponse<>(false, null, error, requestId);
    }
}
