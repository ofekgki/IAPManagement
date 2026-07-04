package com.example.purchasebackend.dto.sdk;

import java.time.Instant;

/** Compact entitlement view used in confirm/restore responses. */
public record EntitlementSummaryDto(
        String entitlementId,
        String status,
        Instant expiresAt
) {
}
