package com.example.purchasebackend.dto.sdk;

/** SDK-facing view of a purchasable item. */
public record ItemDto(
        String itemId,
        String name,
        String description,
        String type,
        String priceDisplay,
        String currency,
        String entitlementId
) {
}
