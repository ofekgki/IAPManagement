package com.example.purchasebackend.dto.sdk;

import java.util.List;

/** Data of POST /api/v1/sdk/purchases/restore. */
public record RestoreResponse(
        List<RestoredPurchaseDto> restoredPurchases,
        List<EntitlementSummaryDto> entitlements
) {
}
