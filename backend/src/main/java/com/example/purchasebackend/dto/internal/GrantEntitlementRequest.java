package com.example.purchasebackend.dto.internal;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

/** Body of POST /api/v1/internal/entitlements/grant. */
public record GrantEntitlementRequest(
        @NotBlank String developerAppId,
        @NotBlank String userId,
        @NotBlank String entitlementId,
        String sourceItemId,
        Instant expiresAt,
        String reason
) {
}
