package com.example.purchasebackend.dto.internal;

import java.time.Instant;

/** Full item view for internal/admin endpoints (includes server-only fields). */
public record AdminItemDto(
        String id,
        String developerAppId,
        String itemId,
        String name,
        String description,
        String type,
        String priceDisplay,
        String currency,
        String googlePlayProductId,
        String entitlementId,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
}
