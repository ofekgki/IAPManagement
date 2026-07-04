package com.example.purchasebackend.dto.internal;

import java.time.Instant;

/** Result of a manual grant/revoke. */
public record EntitlementActionResponse(
        String entitlementRecordId,
        String entitlementId,
        String userId,
        String status,
        Instant expiresAt
) {
}
