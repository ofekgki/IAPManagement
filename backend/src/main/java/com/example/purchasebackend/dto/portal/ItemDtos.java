package com.example.purchasebackend.dto.portal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

/** Portal item DTOs. */
public final class ItemDtos {

    private ItemDtos() {
    }

    public record CreateItemRequest(
            @NotBlank String name,
            // itemId optional: auto-generated (snake_case) from name when blank.
            String itemId,
            String description,
            @NotBlank String type,
            String entitlementId,
            @PositiveOrZero Long priceAmountMinor,
            @NotBlank String currency,
            String priceDisplay,
            String googlePlayProductId,
            Boolean isActive) {
    }

    public record UpdateItemRequest(
            String name,
            String description,
            String entitlementId,
            @PositiveOrZero Long priceAmountMinor,
            String currency,
            String priceDisplay,
            String googlePlayProductId,
            Boolean isActive) {
    }

    public record PortalItemDto(
            String id,
            String itemId,
            String name,
            String description,
            String type,
            String entitlementId,
            Long priceAmountMinor,
            String currency,
            String priceDisplay,
            String googlePlayProductId,
            boolean isActive,
            Instant createdAt,
            Instant updatedAt) {
    }
}
