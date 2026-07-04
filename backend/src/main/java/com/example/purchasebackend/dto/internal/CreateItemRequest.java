package com.example.purchasebackend.dto.internal;

import jakarta.validation.constraints.NotBlank;

/** Body of POST /api/v1/internal/items. */
public record CreateItemRequest(
        @NotBlank String developerAppId,
        @NotBlank String itemId,
        @NotBlank String name,
        String description,
        @NotBlank String type,
        String priceDisplay,
        String currency,
        String googlePlayProductId,
        String entitlementId,
        Boolean isActive
) {
}
