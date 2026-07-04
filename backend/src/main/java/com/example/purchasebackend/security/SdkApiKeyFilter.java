package com.example.purchasebackend.security;

import com.example.purchasebackend.common.ApiException;
import com.example.purchasebackend.common.RequestContext;
import com.example.purchasebackend.service.ApiKeyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Validates the {@code X-SDK-API-Key} header on all {@code /api/v1/sdk/**} requests and publishes the
 * resolved {@link com.example.purchasebackend.domain.DeveloperApp} to {@link RequestContext}.
 *
 * <p>// TODO: Add package name validation.
 * <p>// TODO: Add app signing certificate fingerprint validation.
 * <p>// TODO: Add Play Integrity API verification.
 * <p>// TODO: Add rate limiting per API key.
 */
@Component
@Order(10)
public class SdkApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-SDK-API-Key";
    private static final String SDK_PATH_PREFIX = "/api/v1/sdk";

    private final ApiKeyService apiKeyService;
    private final ObjectMapper objectMapper;

    public SdkApiKeyFilter(ApiKeyService apiKeyService, ObjectMapper objectMapper) {
        this.apiKeyService = apiKeyService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Let CORS preflight through to Spring's CORS handling.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        return !request.getRequestURI().startsWith(SDK_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            RequestContext.setDeveloperApp(apiKeyService.validateAndGetApp(request.getHeader(HEADER)));
        } catch (ApiException ex) {
            FilterSupport.writeError(response, objectMapper, ex.errorCode(), ex.getMessage());
            return;
        }
        filterChain.doFilter(request, response);
    }
}
