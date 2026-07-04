package com.example.purchasebackend.domain;

import com.example.purchasebackend.domain.enums.BillingMode;
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

/**
 * A developer app/project that uses the SDK. Its API keys live in the {@link ApiKey} table (one app
 * can have several keys); the SDK identifies the app by presenting one of those keys.
 *
 * <p>// TODO: In production, validate package name ownership.
 * <p>// TODO: Add app signing certificate fingerprint validation.
 */
@Entity
@Table(name = "developer_app", indexes = {
        @Index(name = "idx_app_owner", columnList = "ownerUserId")
})
public class DeveloperApp {

    @Id
    private String id;

    /** The portal user who owns this app. Null for legacy/seed apps created without a user. */
    private String ownerUserId;

    @Column(nullable = false)
    private String appName;

    private String packageName;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingMode billingModeDefault = BillingMode.MOCK;

    @Column(nullable = false)
    private boolean isActive = true;

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

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public BillingMode getBillingModeDefault() {
        return billingModeDefault;
    }

    public void setBillingModeDefault(BillingMode billingModeDefault) {
        this.billingModeDefault = billingModeDefault;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
