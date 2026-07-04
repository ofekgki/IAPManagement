package com.example.purchasebackend.dto.portal;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

/** API-key DTOs. The raw key is only ever present in {@link CreatedApiKeyResponse}. */
public final class ApiKeyDtos {

    private ApiKeyDtos() {
    }

    public record CreateApiKeyRequest(@NotBlank(message = "Key name is required") String name) {
    }

    public record ApiKeyDto(
            String id,
            String name,
            String keyPrefix,
            String status,
            Instant createdAt,
            Instant revokedAt,
            Instant lastUsedAt) {
    }

    /** Returned exactly once on create/rotate. {@code apiKey} is never retrievable again. */
    public record CreatedApiKeyResponse(String apiKey, String keyPrefix, ApiKeyDto key) {
    }
}
