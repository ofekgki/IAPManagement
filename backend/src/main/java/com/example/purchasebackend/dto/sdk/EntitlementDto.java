package com.example.purchasebackend.dto.sdk;

import java.time.Instant;

/** Full SDK-facing entitlement view used by GET /api/v1/sdk/entitlements. */
public record EntitlementDto(
        String entitlementId,
        String sourceItemId,
        String status,
        Instant expiresAt
) {
}
