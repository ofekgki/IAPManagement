package com.example.purchasebackend.dto.sdk;

/** Data of POST /api/v1/sdk/purchases/start. */
public record StartPurchaseResponse(
        String purchaseId,
        String status,
        String billingMode,
        String itemId
) {
}
