package com.example.purchasebackend.dto.sdk;

/**
 * Google Play purchase details sent with a GOOGLE_PLAY confirmation. Forwarded to
 * {@code GooglePlayVerificationService}; never trusted without server-side verification.
 */
public record GooglePlayConfirmData(
        String purchaseToken,
        String orderId,
        String productId
) {
}
