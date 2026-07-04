package com.example.purchasebackend.dto.sdk;

import java.time.Instant;

/** Data of POST /api/v1/sdk/init. */
public record InitResponse(
        String appId,
        String billingMode,
        Instant serverTime,
        FeaturesDto features
) {
}
