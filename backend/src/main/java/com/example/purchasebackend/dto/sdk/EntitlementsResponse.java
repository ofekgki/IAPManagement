package com.example.purchasebackend.dto.sdk;

import java.util.List;

/** Data of GET /api/v1/sdk/entitlements. */
public record EntitlementsResponse(List<EntitlementDto> entitlements) {
}
