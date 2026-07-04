package com.example.purchasebackend.dto.internal;

/**
 * Body of PATCH /api/v1/internal/items/{itemId}. All fields optional; only non-null fields are
 * applied. {@code type} and {@code itemId} are intentionally not editable.
 */
public record UpdateItemRequest(
        String name,
        String description,
        String priceDisplay,
        String currency,
        String googlePlayProductId,
        String entitlementId,
        Boolean isActive
) {
}
