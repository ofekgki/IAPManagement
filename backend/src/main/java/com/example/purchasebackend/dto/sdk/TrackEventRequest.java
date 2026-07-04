package com.example.purchasebackend.dto.sdk;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/** Body of POST /api/v1/sdk/analytics/events. */
public record TrackEventRequest(
        String userId,
        @NotBlank String eventName,
        String billingMode,
        String itemId,
        String purchaseId,
        Map<String, Object> metadata
) {
}
