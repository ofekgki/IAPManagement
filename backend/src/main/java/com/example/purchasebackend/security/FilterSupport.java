package com.example.purchasebackend.security;

import com.example.purchasebackend.common.ApiError;
import com.example.purchasebackend.common.ApiResponse;
import com.example.purchasebackend.common.ErrorCode;
import com.example.purchasebackend.common.RequestContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;

/** Shared helper for filters to emit the consistent {@link ApiResponse} error envelope as JSON. */
final class FilterSupport {

    private FilterSupport() {
    }

    static void writeError(HttpServletResponse response, ObjectMapper objectMapper,
                           ErrorCode code, String message) throws IOException {
        ApiError error = ApiError.of(code, message, null);
        ApiResponse<Object> body = ApiResponse.fail(error, RequestContext.getRequestId());
        response.setStatus(code.httpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
