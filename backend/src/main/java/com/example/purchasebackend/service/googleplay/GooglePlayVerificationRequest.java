package com.example.purchasebackend.service.googleplay;

import com.example.purchasebackend.domain.enums.ItemType;

/** Everything {@code GooglePlayVerificationService} needs to verify a purchase server-side. */
public record GooglePlayVerificationRequest(
        String developerAppId,
        String packageName,
        String productId,
        String purchaseToken,
        String orderId,
        String userId,
        ItemType itemType
) {
}
