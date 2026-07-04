package com.example.purchasebackend.dto.sdk;

import java.time.Instant;

/** Data of GET /api/v1/sdk/entitlements/check. */
public record CheckEntitlementResponse(
        boolean hasEntitlement,
        String entitlementId,
        String status,
        Instant expiresAt
) {
}
