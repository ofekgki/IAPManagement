package com.example.purchasebackend.security;

import com.example.purchasebackend.common.ErrorCode;
import com.example.purchasebackend.domain.DeveloperUser;
import com.example.purchasebackend.repository.DeveloperUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * JWT authentication for {@code /api/v1/portal/**} (except the public auth endpoints). Verifies the
 * {@code Authorization: Bearer <jwt>} token, loads the {@link DeveloperUser}, and publishes it to
 * {@link PortalContext}.
 *
 * <p>// TODO: Add refresh-token handling and token revocation.
 */
@Component
@Order(12)
public class PortalAuthFilter extends OncePerRequestFilter {

    private static final String PORTAL_PREFIX = "/api/v1/portal";

    private final JwtService jwtService;
    private final DeveloperUserRepository userRepository;
    private final ObjectMapper objectMapper;

    public PortalAuthFilter(JwtService jwtService, DeveloperUserRepository userRepository,
                            ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Let CORS preflight through to Spring's CORS handling.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        if (!uri.startsWith(PORTAL_PREFIX)) {
            return true;
        }
        // Public: register + login. (logout/me require a token.)
        return uri.equals(PORTAL_PREFIX + "/auth/register") || uri.equals(PORTAL_PREFIX + "/auth/login");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            FilterSupport.writeError(response, objectMapper, ErrorCode.UNAUTHORIZED, null);
            return;
        }
        Map<String, Object> claims = jwtService.verify(header.substring("Bearer ".length()).trim());
        if (claims == null) {
            FilterSupport.writeError(response, objectMapper, ErrorCode.UNAUTHORIZED, "Invalid or expired token.");
            return;
        }
        Optional<DeveloperUser> user = userRepository.findById(String.valueOf(claims.get("sub")));
        if (user.isEmpty()) {
            FilterSupport.writeError(response, objectMapper, ErrorCode.UNAUTHORIZED, null);
            return;
        }
        try {
            PortalContext.setUser(user.get());
            filterChain.doFilter(request, response);
        } finally {
            PortalContext.clear();
        }
    }
}
