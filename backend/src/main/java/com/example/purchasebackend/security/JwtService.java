package com.example.purchasebackend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

/**
 * Minimal, dependency-free HS256 JWT (header.payload.signature). Used for portal authentication.
 *
 * <p>The signing secret comes from {@code app.jwt-secret} (env {@code JWT_SECRET}). Tokens carry the
 * user id ({@code sub}), email, role, and an expiry ({@code exp}).
 *
 * <p>// TODO: Add refresh tokens and token revocation/blacklist.
 */
@Component
public class JwtService {

    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    private final byte[] secret;
    private final long ttlSeconds;
    private final ObjectMapper objectMapper;

    public JwtService(@Value("${app.jwt-secret:}") String secret,
                      @Value("${app.jwt-ttl-seconds:86400}") long ttlSeconds,
                      ObjectMapper objectMapper) {
        // Fall back to a clearly-insecure dev default so the app boots without config. NEVER use in prod.
        String effective = (secret == null || secret.isBlank())
                ? "dev-insecure-jwt-secret-change-me-please-0123456789" : secret;
        this.secret = effective.getBytes(StandardCharsets.UTF_8);
        this.ttlSeconds = ttlSeconds;
        this.objectMapper = objectMapper;
    }

    public String issue(String userId, String email, String role) {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> claims = Map.of(
                "sub", userId,
                "email", email,
                "role", role,
                "iat", now,
                "exp", now + ttlSeconds);
        String header = B64.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = B64.encodeToString(toJson(claims).getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + payload;
        String signature = B64.encodeToString(hmac(signingInput.getBytes(StandardCharsets.UTF_8)));
        return signingInput + "." + signature;
    }

    /** Verifies signature + expiry and returns the claims, or null if invalid/expired. */
    public Map<String, Object> verify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            String signingInput = parts[0] + "." + parts[1];
            byte[] expectedSig = hmac(signingInput.getBytes(StandardCharsets.UTF_8));
            byte[] actualSig = B64D.decode(parts[2]);
            if (!java.security.MessageDigest.isEqual(expectedSig, actualSig)) {
                return null;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = objectMapper.readValue(B64D.decode(parts[1]), Map.class);
            Object exp = claims.get("exp");
            long expSeconds = exp instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(exp));
            if (Instant.now().getEpochSecond() >= expSeconds) {
                return null;
            }
            return claims;
        } catch (Exception ex) {
            return null;
        }
    }

    private byte[] hmac(byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign JWT", e);
        }
    }

    private String toJson(Map<String, Object> claims) {
        try {
            return objectMapper.writeValueAsString(claims);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to serialize JWT claims", e);
        }
    }
}
