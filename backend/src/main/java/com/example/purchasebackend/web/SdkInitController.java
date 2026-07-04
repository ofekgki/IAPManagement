package com.example.purchasebackend.web;

import com.example.purchasebackend.common.ApiResponse;
import com.example.purchasebackend.common.RequestContext;
import com.example.purchasebackend.domain.DeveloperApp;
import com.example.purchasebackend.domain.enums.BillingMode;
import com.example.purchasebackend.dto.sdk.FeaturesDto;
import com.example.purchasebackend.dto.sdk.InitRequest;
import com.example.purchasebackend.dto.sdk.InitResponse;
import com.example.purchasebackend.service.AnalyticsService;
import com.example.purchasebackend.service.support.BillingModes;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** POST /api/v1/sdk/init — validates the app, returns feature flags, records sdk_initialized. */
@RestController
@RequestMapping("/api/v1/sdk")
public class SdkInitController {

    /** Flip to true only once real Google Play verification is implemented and configured. */
    private static final boolean GOOGLE_PLAY_BILLING_ENABLED = false;

    private final AnalyticsService analyticsService;

    public SdkInitController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @PostMapping("/init")
    public ApiResponse<InitResponse> init(@RequestBody InitRequest request) {
        DeveloperApp app = RequestContext.requireDeveloperApp();

        // TODO: Validate packageName against the registered developer app.
        // TODO: Validate signing certificate fingerprint.
        // TODO: Add Play Integrity API validation.

        BillingMode requestedMode = BillingModes.parseOrDefault(request.billingMode(), app.getBillingModeDefault());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("sdkVersion", request.sdkVersion());
        metadata.put("packageName", request.packageName());
        analyticsService.record(app.getId(), request.userId(), "sdk_initialized",
                requestedMode, null, null, metadata);

        FeaturesDto features = new FeaturesDto(
                true,                          // mock billing always available
                GOOGLE_PLAY_BILLING_ENABLED,   // false until verification is configured
                true);                         // analytics enabled

        InitResponse data = new InitResponse(app.getId(), requestedMode.name(), Instant.now(), features);
        return ApiResponse.ok(data, RequestContext.getRequestId());
    }
}
