package com.example.purchasebackend.domain.enums;

/** Commercial nature of a purchasable item. Drives entitlement semantics. */
public enum ItemType {
    /** Owned once, forever (e.g. remove ads). Grants a non-expiring entitlement. */
    NON_CONSUMABLE,
    /** Used up and re-buyable (e.g. coins). Typically no long-lived entitlement. */
    CONSUMABLE,
    /** Time-limited access (e.g. monthly). Grants an entitlement with an expiry. */
    SUBSCRIPTION
}
