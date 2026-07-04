package com.example.purchasebackend.domain;

import com.example.purchasebackend.domain.enums.ApiKeyStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * An API key belonging to one {@link DeveloperApp}. The raw key is shown once at creation; only a
 * deterministic hash ({@code keyHash}) is stored so the SDK key can be looked up on each request.
 *
 * <p>// TODO: Add rate limiting per API key.
 */
@Entity
@Table(name = "api_key", indexes = {
        @Index(name = "idx_apikey_hash", columnList = "keyHash", unique = true),
        @Index(name = "idx_apikey_app", columnList = "developerAppId")
})
public class ApiKey {

    @Id
    private String id;

    @Column(nullable = false)
    private String developerAppId;

    private String name;

    /** Non-secret prefix shown in the UI, e.g. "psdk_live_abc123". */
    @Column(nullable = false)
    private String keyPrefix;

    /** Deterministic SHA-256(pepper + rawKey) hex. Used to resolve the key on SDK requests. */
    @Column(nullable = false, unique = true)
    private String keyHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApiKeyStatus status = ApiKeyStatus.ACTIVE;

    private Instant createdAt;
    private Instant revokedAt;
    private Instant lastUsedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDeveloperAppId() {
        return developerAppId;
    }

    public void setDeveloperAppId(String developerAppId) {
        this.developerAppId = developerAppId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public void setKeyHash(String keyHash) {
        this.keyHash = keyHash;
    }

    public ApiKeyStatus getStatus() {
        return status;
    }

    public void setStatus(ApiKeyStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}
