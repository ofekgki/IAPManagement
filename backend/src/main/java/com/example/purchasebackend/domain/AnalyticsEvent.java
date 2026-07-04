package com.example.purchasebackend.domain;

import com.example.purchasebackend.domain.enums.BillingMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/** A stored analytics event. Metadata is kept as opaque JSON. */
@Entity
@Table(name = "analytics_event", indexes = {
        @Index(name = "idx_evt_app_name_time", columnList = "developerAppId,eventName,createdAt"),
        // Serves the events-in-window fetch (app + createdAt range without an eventName).
        @Index(name = "idx_evt_app_time", columnList = "developerAppId,createdAt"),
        // Serves the purchase-detail timeline (findByPurchaseId) — otherwise a full table scan.
        @Index(name = "idx_evt_purchase", columnList = "purchaseId")
})
public class AnalyticsEvent {

    @Id
    private String id;

    @Column(nullable = false)
    private String developerAppId;

    private String userId;

    @Column(nullable = false)
    private String eventName;

    @Enumerated(EnumType.STRING)
    private BillingMode billingMode;

    private String itemId;
    private String purchaseId;

    // Plain text column (NOT @Lob): on PostgreSQL @Lob String maps to an `oid` large object, which
    // can't be read in auto-commit mode and 500s the analytics reads. A sized varchar is portable and
    // more than enough for small JSON metadata.
    @Column(length = 4000)
    private String metadataJson;

    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
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

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public BillingMode getBillingMode() {
        return billingMode;
    }

    public void setBillingMode(BillingMode billingMode) {
        this.billingMode = billingMode;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getPurchaseId() {
        return purchaseId;
    }

    public void setPurchaseId(String purchaseId) {
        this.purchaseId = purchaseId;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
