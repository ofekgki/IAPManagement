package com.example.purchasebackend.domain;

import com.example.purchasebackend.domain.enums.BillingMode;
import com.example.purchasebackend.domain.enums.BillingProviderType;
import com.example.purchasebackend.domain.enums.PaymentMethod;
import com.example.purchasebackend.domain.enums.PurchaseStatus;
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
 * A purchase attempt and its result.
 *
 * <p>// Log only a shortened or hashed Google purchase token, never the full token (see
 * PurchaseService). The {@code providerPurchaseToken} column stores the raw token for verification
 * but it must never be logged in full.
 */
@Entity
@Table(name = "purchase", indexes = {
        @Index(name = "idx_purchase_app_user", columnList = "developerAppId,userId"),
        // Serves the analytics window queries (app + status + completedAt range) index-only.
        @Index(name = "idx_purchase_app_status_completed", columnList = "developerAppId,status,completedAt"),
        // Serves the portal purchases list / purchases-by-status window (app + createdAt range).
        @Index(name = "idx_purchase_app_created", columnList = "developerAppId,createdAt")
})
public class Purchase {

    @Id
    private String id;

    @Column(nullable = false)
    private String developerAppId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String itemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingMode billingMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PurchaseStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingProviderType provider;

    /**
     * How the user paid (Apple Pay / Google Play / PayPal / Credit Card). The portal's revenue
     * breakdown groups on this. Nullable for legacy rows.
     */
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private String providerPurchaseToken;
    private String providerOrderId;

    private String idempotencyKey;

    private String failureCode;
    private String failureMessage;

    /**
     * Price snapshot taken when the purchase is started. Revenue is computed from this value so a
     * later edit of the item's list price never rewrites historical revenue. Nullable for legacy
     * rows — readers fall back to the item's current price when null.
     */
    private Long priceAmountMinor;
    private String priceCurrency;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
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

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public BillingMode getBillingMode() {
        return billingMode;
    }

    public void setBillingMode(BillingMode billingMode) {
        this.billingMode = billingMode;
    }

    public PurchaseStatus getStatus() {
        return status;
    }

    public void setStatus(PurchaseStatus status) {
        this.status = status;
    }

    public BillingProviderType getProvider() {
        return provider;
    }

    public void setProvider(BillingProviderType provider) {
        this.provider = provider;
    }

    public String getProviderPurchaseToken() {
        return providerPurchaseToken;
    }

    public void setProviderPurchaseToken(String providerPurchaseToken) {
        this.providerPurchaseToken = providerPurchaseToken;
    }

    public String getProviderOrderId() {
        return providerOrderId;
    }

    public void setProviderOrderId(String providerOrderId) {
        this.providerOrderId = providerOrderId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public void setFailureCode(String failureCode) {
        this.failureCode = failureCode;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }

    public Long getPriceAmountMinor() {
        return priceAmountMinor;
    }

    public void setPriceAmountMinor(Long priceAmountMinor) {
        this.priceAmountMinor = priceAmountMinor;
    }

    public String getPriceCurrency() {
        return priceCurrency;
    }

    public void setPriceCurrency(String priceCurrency) {
        this.priceCurrency = priceCurrency;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
