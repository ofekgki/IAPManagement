package com.example.purchasebackend.service;

import com.example.purchasebackend.common.ApiException;
import com.example.purchasebackend.common.ErrorCode;
import com.example.purchasebackend.common.Ids;
import com.example.purchasebackend.domain.ApiKey;
import com.example.purchasebackend.domain.DeveloperApp;
import com.example.purchasebackend.domain.enums.ApiKeyStatus;
import com.example.purchasebackend.repository.ApiKeyRepository;
import com.example.purchasebackend.repository.DeveloperAppRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

/**
 * Issues and validates SDK API keys (stored in the {@link ApiKey} table).
 *
 * <p>The raw key is shown to the developer once at creation; the DB stores only a deterministic
 * {@code SHA-256(pepper + rawKey)} hash so the key can be resolved on each SDK request. The key
 * identifies the app — it is NOT a strong secret (it ships inside a client app).
 *
 * <p>// TODO: Add Play Integrity API verification.
 * <p>// TODO: Add package name validation.
 * <p>// TODO: Add certificate fingerprint validation.
 * <p>// TODO: Add rate limiting per API key.
 */
@Service
public class ApiKeyService {

    private static final String KEY_PREFIX = "psdk_live_";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ApiKeyRepository apiKeyRepository;
    private final DeveloperAppRepository developerAppRepository;
    private final String pepper;

    public ApiKeyService(ApiKeyRepository apiKeyRepository,
                         DeveloperAppRepository developerAppRepository,
                         @Value("${app.api-key-pepper:}") String pepper) {
        this.apiKeyRepository = apiKeyRepository;
        this.developerAppRepository = developerAppRepository;
        this.pepper = pepper;
    }

    /** Result of creating a key: the raw value (shown once) plus the stored record. */
    public record CreatedApiKey(String rawApiKey, ApiKey apiKey) {
    }

    /** Deterministic hash used for storage and lookup. */
    public String hashApiKey(String rawApiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((pepper + rawApiKey).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Resolves and validates the app for a raw API key, and stamps {@code lastUsedAt}.
     *
     * @throws ApiException INVALID_API_KEY (missing/unknown/revoked), APP_DISABLED (inactive app).
     */
    public DeveloperApp validateAndGetApp(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_API_KEY);
        }
        ApiKey key = apiKeyRepository.findByKeyHash(hashApiKey(rawApiKey))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_API_KEY));
        if (key.getStatus() != ApiKeyStatus.ACTIVE) {
            throw new ApiException(ErrorCode.INVALID_API_KEY, "This API key has been revoked.");
        }
        DeveloperApp app = developerAppRepository.findById(key.getDeveloperAppId())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_API_KEY));
        if (!app.isActive()) {
            throw new ApiException(ErrorCode.APP_DISABLED);
        }
        key.setLastUsedAt(Instant.now());
        apiKeyRepository.save(key);
        return app;
    }

    /** Creates a new API key for an app. The returned raw value is the only time it is visible. */
    public CreatedApiKey create(String developerAppId, String name) {
        String finalName = name != null && !name.isBlank() ? name.trim() : "default";
        // No two ACTIVE keys in the same app may share a name (case-insensitive). A revoked key's name
        // can be reused (rotate() revokes the old key first, then re-creates with the same name).
        boolean duplicate = apiKeyRepository.findByDeveloperAppIdOrderByCreatedAtDesc(developerAppId).stream()
                .anyMatch(k -> k.getStatus() == ApiKeyStatus.ACTIVE && finalName.equalsIgnoreCase(k.getName()));
        if (duplicate) {
            throw new ApiException(ErrorCode.INVALID_REQUEST,
                    "An active API key named '" + finalName + "' already exists for this app.");
        }
        String raw = generateRawKey();
        ApiKey key = new ApiKey();
        key.setId(Ids.newId("key"));
        key.setDeveloperAppId(developerAppId);
        key.setName(finalName);
        key.setKeyPrefix(raw.substring(0, KEY_PREFIX.length() + 6)); // e.g. psdk_live_abc123
        key.setKeyHash(hashApiKey(raw));
        key.setStatus(ApiKeyStatus.ACTIVE);
        apiKeyRepository.save(key);
        return new CreatedApiKey(raw, key);
    }

    public List<ApiKey> list(String developerAppId) {
        return apiKeyRepository.findByDeveloperAppIdOrderByCreatedAtDesc(developerAppId);
    }

    public ApiKey revoke(String developerAppId, String keyId) {
        ApiKey key = getOwned(developerAppId, keyId);
        key.setStatus(ApiKeyStatus.REVOKED);
        key.setRevokedAt(Instant.now());
        return apiKeyRepository.save(key);
    }

    /** Revokes an existing key and issues a fresh one (same name). */
    public CreatedApiKey rotate(String developerAppId, String keyId) {
        ApiKey old = getOwned(developerAppId, keyId);
        old.setStatus(ApiKeyStatus.REVOKED);
        old.setRevokedAt(Instant.now());
        apiKeyRepository.save(old);
        return create(developerAppId, old.getName());
    }

    private ApiKey getOwned(String developerAppId, String keyId) {
        return apiKeyRepository.findByIdAndDeveloperAppId(keyId, developerAppId)
                .orElseThrow(() -> new ApiException(ErrorCode.API_KEY_NOT_FOUND));
    }

    private String generateRawKey() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        String body = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
                .replaceAll("[-_]", "");
        return KEY_PREFIX + body;
    }
}
