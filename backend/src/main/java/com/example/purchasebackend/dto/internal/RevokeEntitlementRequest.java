package com.example.purchasebackend.dto.internal;

import jakarta.validation.constraints.NotBlank;

/** Body of POST /api/v1/internal/entitlements/revoke. */
public record RevokeEntitlementRequest(
        @NotBlank String developerAppId,
        @NotBlank String userId,
        @NotBlank String entitlementId,
        String reason
) {
}
