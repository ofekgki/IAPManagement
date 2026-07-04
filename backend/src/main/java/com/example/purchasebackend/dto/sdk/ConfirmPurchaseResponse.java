package com.example.purchasebackend.dto.sdk;

/** Data of POST /api/v1/sdk/purchases/confirm. */
public record ConfirmPurchaseResponse(
        String purchaseId,
        String status,
        boolean entitlementGranted,
        EntitlementSummaryDto entitlement
) {
}
