package com.example.purchasebackend.dto.sdk;

/**
 * A previously completed purchase, as returned by restore.
 *
 * @param status            the purchase status ("RESTORED" — the purchase was re-synced by restore).
 * @param entitlementStatus the CURRENT access for this item ("ACTIVE" / "REVOKED" / "EXPIRED"), or
 *                          null when the item grants no long-lived entitlement (e.g. a consumable).
 *                          Restore never changes this — a revoked entitlement stays REVOKED.
 */
public record RestoredPurchaseDto(
        String purchaseId,
        String itemId,
        String itemName,
        String status,
        String entitlementStatus
) {
}
