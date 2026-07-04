package com.example.purchasebackend.web;

import com.example.purchasebackend.common.ApiResponse;
import com.example.purchasebackend.common.RequestContext;
import com.example.purchasebackend.dto.internal.AnalyticsSummaryResponse;
import com.example.purchasebackend.service.AnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/** Internal/admin basic analytics counts. No dashboard UI. Guarded by {@code InternalAdminTokenFilter}. */
@RestController
@RequestMapping("/api/v1/internal/analytics")
public class InternalAnalyticsController {

    private final AnalyticsService analyticsService;

    public InternalAnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /** GET /internal/analytics/summary?developerAppId&from=yyyy-MM-dd&to=yyyy-MM-dd */
    @GetMapping("/summary")
    public ApiResponse<AnalyticsSummaryResponse> summary(
            @RequestParam String developerAppId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Instant fromInstant = from != null ? from.atStartOfDay(ZoneOffset.UTC).toInstant() : Instant.EPOCH;
        Instant toInstant = to != null
                ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
                : Instant.now().plusSeconds(60);
        AnalyticsSummaryResponse data = analyticsService.summary(developerAppId, fromInstant, toInstant);
        return ApiResponse.ok(data, RequestContext.getRequestId());
    }
}
