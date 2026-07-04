package com.example.purchasebackend.dto.sdk;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of POST /api/v1/sdk/purchases/restore.
 *
 * @param itemId when non-blank, only this single item is returned/refunded (per-item return).
 *               When null/blank, every currently-owned item is returned (restore all).
 */
public record RestoreRequest(
        @NotBlank String userId,
        String billingMode,
        String itemId
) {
}
