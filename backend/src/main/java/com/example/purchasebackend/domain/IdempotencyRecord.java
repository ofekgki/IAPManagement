package com.example.purchasebackend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * Stores the result of an idempotent operation (purchase confirmation) keyed by
 * (developerAppId, idempotencyKey), so a repeated request returns the original response instead of
 * processing twice.
 */
@Entity
@Table(name = "idempotency_record",
        uniqueConstraints = @UniqueConstraint(name = "uq_idem_app_key", columnNames = {"developerAppId", "idempotencyKey"}))
public class IdempotencyRecord {

    @Id
    private String id;

    @Column(nullable = false)
    private String developerAppId;

    @Column(nullable = false)
    private String idempotencyKey;

    private String purchaseId;

    /**
     * The serialized response returned the first time this key was processed. Plain text column (NOT
     * @Lob): on PostgreSQL @Lob String maps to an `oid` large object that can't be read in auto-commit
     * mode; a sized varchar is portable and large enough for a confirm response.
     */
    @Column(length = 8000)
    private String responseJson;

    private Instant createdAt;

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

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getPurchaseId() {
        return purchaseId;
    }

    public void setPurchaseId(String purchaseId) {
        this.purchaseId = purchaseId;
    }

    public String getResponseJson() {
        return responseJson;
    }

    public void setResponseJson(String responseJson) {
        this.responseJson = responseJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
