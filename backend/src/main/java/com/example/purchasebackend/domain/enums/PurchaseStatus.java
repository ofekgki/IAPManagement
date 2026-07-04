package com.example.purchasebackend.domain.enums;

/** Lifecycle state of a purchase record. */
public enum PurchaseStatus {
    CREATED,
    PENDING,
    SUCCESS,
    FAILED,
    CANCELLED,
    REQUIRES_VERIFICATION,
    /** A prior purchase re-granted to the user via restorePurchases (carries no new revenue). */
    RESTORED
}
