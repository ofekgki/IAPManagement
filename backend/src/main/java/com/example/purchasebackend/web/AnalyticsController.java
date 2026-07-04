package com.example.purchasebackend.web;

import com.example.purchasebackend.common.ApiResponse;
import com.example.purchasebackend.common.RequestContext;
import com.example.purchasebackend.domain.DeveloperApp;
import com.example.purchasebackend.domain.enums.BillingMode;
import com.example.purchasebackend.dto.sdk.TrackEventRequest;
import com.example.purchasebackend.dto.sdk.TrackEventResponse;
import com.example.purchasebackend.service.AnalyticsService;
import com.example.purchasebackend.service.support.BillingModes;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** SDK analytics ingestion. Storage failures never fail the request flow (handled in the service). */
@RestController
@RequestMapping("/api/v1/sdk/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @PostMapping("/events")
    public ApiResponse<TrackEventResponse> track(@Valid @RequestBody TrackEventRequest request) {
        DeveloperApp app = RequestContext.requireDeveloperApp();
        BillingMode billingMode = request.billingMode() == null ? null
                : BillingModes.parseOrDefault(request.billingMode(), app.getBillingModeDefault());
        boolean stored = analyticsService.record(
                app.getId(), request.userId(), request.eventName(), billingMode,
                request.itemId(), request.purchaseId(), request.metadata());
        return ApiResponse.ok(new TrackEventResponse(stored), RequestContext.getRequestId());
    }
}
