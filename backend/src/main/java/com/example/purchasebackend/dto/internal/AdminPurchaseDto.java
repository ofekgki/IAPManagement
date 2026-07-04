package com.example.purchasebackend.dto.internal;

import java.time.Instant;

/** Internal/admin view of a purchase. Note: raw provider tokens are intentionally NOT included. */
public record AdminPurchaseDto(
        String purchaseId,
        String developerAppId,
        String userId,
        String itemId,
        String billingMode,
        String status,
        String provider,
        String providerOrderId,
        String idempotencyKey,
        String failureCode,
        Instant createdAt,
        Instant completedAt
) {
}
