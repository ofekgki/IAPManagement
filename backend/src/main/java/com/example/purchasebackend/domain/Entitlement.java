package com.example.purchasebackend.domain;

import com.example.purchasebackend.domain.enums.EntitlementStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

/** Access granted to a user for a given entitlement id. */
@Entity
@Table(name = "entitlement", indexes = {
        @Index(name = "idx_ent_app_user", columnList = "developerAppId,userId"),
        @Index(name = "idx_ent_app_user_ent", columnList = "developerAppId,userId,entitlementId"),
        // Serves countByDeveloperAppIdAndStatus (the portal's "Active entitlements" KPI).
        @Index(name = "idx_ent_app_status", columnList = "developerAppId,status")
})
public class Entitlement {

    @Id
    private String id;

    @Column(nullable = false)
    private String developerAppId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String entitlementId;

    private String sourceItemId;
    private String purchaseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntitlementStatus status;

    private Instant startsAt;
    /** Null means non-expiring (e.g. non-consumable). */
    private Instant expiresAt;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEntitlementId() {
        return entitlementId;
    }

    public void setEntitlementId(String entitlementId) {
        this.entitlementId = entitlementId;
    }

    public String getSourceItemId() {
        return sourceItemId;
    }

    public void setSourceItemId(String sourceItemId) {
        this.sourceItemId = sourceItemId;
    }

    public String getPurchaseId() {
        return purchaseId;
    }

    public void setPurchaseId(String purchaseId) {
        this.purchaseId = purchaseId;
    }

    public EntitlementStatus getStatus() {
        return status;
    }

    public void setStatus(EntitlementStatus status) {
        this.status = status;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(Instant startsAt) {
        this.startsAt = startsAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
