package com.example.purchasebackend.dto.sdk;

/** Feature flags returned from init so the SDK knows what the backend currently supports. */
public record FeaturesDto(
        boolean mockBillingEnabled,
        boolean googlePlayBillingEnabled,
        boolean analyticsEnabled
) {
}
