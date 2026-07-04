package com.example.purchasebackend.dto.portal;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

/** Portal entitlement DTOs. */
public final class EntitlementDtos {

    private EntitlementDtos() {
    }

    public record PortalEntitlementDto(
            String id,
            String userId,
            String entitlementId,
            String sourceItemId,
            String status,
            Instant startsAt,
            Instant expiresAt,
            String purchaseId) {
    }

    public record GrantRequest(
            @NotBlank String userId,
            @NotBlank String entitlementId,
            String sourceItemId,
            Instant expiresAt,
            String reason) {
    }

    public record RevokeRequest(
            @NotBlank String userId,
            @NotBlank String entitlementId,
            String reason) {
    }
}
