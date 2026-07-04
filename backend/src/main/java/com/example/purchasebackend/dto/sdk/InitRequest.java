package com.example.purchasebackend.dto.sdk;

/** Body of POST /api/v1/sdk/init. */
public record InitRequest(
        String sdkVersion,
        String packageName,
        String userId,
        String billingMode
) {
}
