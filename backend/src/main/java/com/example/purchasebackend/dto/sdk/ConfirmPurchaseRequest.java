package com.example.purchasebackend.dto.sdk;

import jakarta.validation.constraints.NotBlank;

/** Body of POST /api/v1/sdk/purchases/confirm. {@code googlePlay} is only set in GOOGLE_PLAY mode. */
public record ConfirmPurchaseRequest(
        @NotBlank String purchaseId,
        @NotBlank String userId,
        @NotBlank String itemId,
        String billingMode,
        GooglePlayConfirmData googlePlay
) {
}
