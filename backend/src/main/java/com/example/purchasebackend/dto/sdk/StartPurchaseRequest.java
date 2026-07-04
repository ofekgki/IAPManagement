package com.example.purchasebackend.dto.sdk;

import jakarta.validation.constraints.NotBlank;

/** Body of POST /api/v1/sdk/purchases/start. */
public record StartPurchaseRequest(
        @NotBlank String userId,
        @NotBlank String itemId,
        String billingMode,
        /** How the user chose to pay (APPLE_PAY / GOOGLE_PLAY / PAYPAL / CREDIT_CARD). Optional. */
        String paymentMethod
) {
}
