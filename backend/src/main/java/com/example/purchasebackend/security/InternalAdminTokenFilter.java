package com.example.purchasebackend.security;

import com.example.purchasebackend.common.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Guards all {@code /api/v1/internal/**} endpoints with a shared admin token
 * ({@code X-Internal-Admin-Token}).
 *
 * <p><b>Fails closed:</b> if no admin token is configured, every internal request is rejected.
 *
 * <p>// TODO: Replace this shared-token check with proper authentication/authorization (e.g. mTLS,
 * OAuth2 client credentials, or an API gateway). Do NOT expose internal endpoints publicly in prod.
 */
@Component
@Order(11)
public class InternalAdminTokenFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Admin-Token";
    private static final String INTERNAL_PATH_PREFIX = "/api/v1/internal";

    private final String configuredToken;
    private final ObjectMapper objectMapper;

    public InternalAdminTokenFilter(@Value("${app.internal-admin-token:}") String configuredToken,
                                    ObjectMapper objectMapper) {
        this.configuredToken = configuredToken;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        return !request.getRequestURI().startsWith(INTERNAL_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        boolean ok = configuredToken != null && !configuredToken.isBlank()
                && configuredToken.equals(provided);
        if (!ok) {
            FilterSupport.writeError(response, objectMapper, ErrorCode.ADMIN_TOKEN_INVALID, null);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
