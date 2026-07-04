package com.example.purchasebackend.domain;

import com.example.purchasebackend.domain.enums.ItemType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * A purchasable item belonging to a developer app.
 *
 * <p>// TODO: In GOOGLE_PLAY mode, prices should be loaded from Google Play ProductDetails instead of
 * trusting local server values. {@code priceDisplay}/{@code currency} here are for MOCK/demo display.
 */
@Entity
@Table(name = "purchase_item",
        uniqueConstraints = @UniqueConstraint(name = "uq_item_app_itemid", columnNames = {"developerAppId", "itemId"}),
        indexes = @Index(name = "idx_item_app", columnList = "developerAppId"))
public class PurchaseItem {

    @Id
    private String id;

    @Column(nullable = false)
    private String developerAppId;

    /** SDK-facing product id, e.g. "remove_ads". Unique per developer app. */
    @Column(nullable = false)
    private String itemId;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemType type;

    /** Display price for MOCK/demo only, e.g. "$1.99". */
    private String priceDisplay;
    private String currency;

    /** Authoritative price in minor units (e.g. cents). Used for revenue analytics, not priceDisplay. */
    private Long priceAmountMinor;

    /** Required later for GOOGLE_PLAY mode; null in MOCK. */
    private String googlePlayProductId;

    /** The entitlement this item grants, e.g. "ent_remove_ads". May be null for pure consumables. */
    private String entitlementId;

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

    public String getDeveloperAppId() {
        return developerAppId;
    }

    public void setDeveloperAppId(String developerAppId) {
        this.developerAppId = developerAppId;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ItemType getType() {
        return type;
    }

    public void setType(ItemType type) {
        this.type = type;
    }

    public String getPriceDisplay() {
        return priceDisplay;
    }

    public void setPriceDisplay(String priceDisplay) {
        this.priceDisplay = priceDisplay;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Long getPriceAmountMinor() {
        return priceAmountMinor;
    }

    public void setPriceAmountMinor(Long priceAmountMinor) {
        this.priceAmountMinor = priceAmountMinor;
    }

    public String getGooglePlayProductId() {
        return googlePlayProductId;
    }

    public void setGooglePlayProductId(String googlePlayProductId) {
        this.googlePlayProductId = googlePlayProductId;
    }

    public String getEntitlementId() {
        return entitlementId;
    }

    public void setEntitlementId(String entitlementId) {
        this.entitlementId = entitlementId;
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
