package com.example.purchasebackend.web.portal;

import com.example.purchasebackend.common.ApiResponse;
import com.example.purchasebackend.common.RequestContext;
import com.example.purchasebackend.dto.portal.AnalyticsDtos.EventDto;
import com.example.purchasebackend.dto.portal.AnalyticsDtos.FunnelResponse;
import com.example.purchasebackend.dto.portal.AnalyticsDtos.OverviewResponse;
import com.example.purchasebackend.dto.portal.AnalyticsDtos.PurchaseStatusRow;
import com.example.purchasebackend.dto.portal.AnalyticsDtos.RevenueByProductRow;
import com.example.purchasebackend.dto.portal.AnalyticsDtos.RevenueByTimePoint;
import com.example.purchasebackend.dto.portal.AnalyticsDtos.RevenueSummaryResponse;
import com.example.purchasebackend.domain.enums.PaymentMethod;
import com.example.purchasebackend.security.PortalContext;
import com.example.purchasebackend.service.portal.PortalAnalyticsService;
import com.example.purchasebackend.service.portal.PortalAppService;
import com.example.purchasebackend.service.support.DateRanges;
import com.example.purchasebackend.service.support.PaymentMethods;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Portal analytics + revenue (JWT). Default date range is the last 30 days. */
@RestController
@RequestMapping("/api/v1/portal/apps/{appId}/analytics")
public class PortalAnalyticsController {

    private final PortalAnalyticsService analyticsService;
    private final PortalAppService appService;

    public PortalAnalyticsController(PortalAnalyticsService analyticsService, PortalAppService appService) {
        this.analyticsService = analyticsService;
        this.appService = appService;
    }

    @GetMapping("/overview")
    public ApiResponse<OverviewResponse> overview(
            @PathVariable String appId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String itemId,
            @RequestParam(required = false) String paymentMethod) {
        authorize(appId);
        OverviewResponse data = analyticsService.overview(appId, from(from), to(to), itemId, method(paymentMethod));
        return ApiResponse.ok(data, RequestContext.getRequestId());
    }

    @GetMapping("/funnel")
    public ApiResponse<FunnelResponse> funnel(
            @PathVariable String appId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String itemId,
            @RequestParam(required = false) String paymentMethod) {
        authorize(appId);
        FunnelResponse data = analyticsService.funnel(appId, from(from), to(to), itemId, method(paymentMethod));
        return ApiResponse.ok(data, RequestContext.getRequestId());
    }

    @GetMapping("/revenue")
    public ApiResponse<RevenueSummaryResponse> revenue(
            @PathVariable String appId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String itemId,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false, defaultValue = "day") String groupBy) {
        authorize(appId);
        RevenueSummaryResponse data = analyticsService.revenue(
                appId, from(from), to(to), itemId, method(paymentMethod), groupBy);
        return ApiResponse.ok(data, RequestContext.getRequestId());
    }

    @GetMapping("/revenue/by-product")
    public ApiResponse<List<RevenueByProductRow>> revenueByProduct(
            @PathVariable String appId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String paymentMethod) {
        authorize(appId);
        List<RevenueByProductRow> data = analyticsService.revenueByProduct(
                appId, from(from), to(to), method(paymentMethod));
        return ApiResponse.ok(data, RequestContext.getRequestId());
    }

    @GetMapping("/revenue/by-time")
    public ApiResponse<List<RevenueByTimePoint>> revenueByTime(
            @PathVariable String appId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String itemId,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false, defaultValue = "day") String groupBy) {
        authorize(appId);
        List<RevenueByTimePoint> data = analyticsService.revenueByTime(
                appId, from(from), to(to), itemId, method(paymentMethod), groupBy);
        return ApiResponse.ok(data, RequestContext.getRequestId());
    }

    @GetMapping("/purchases-by-status")
    public ApiResponse<List<PurchaseStatusRow>> purchasesByStatus(
            @PathVariable String appId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String itemId,
            @RequestParam(required = false) String paymentMethod) {
        authorize(appId);
        List<PurchaseStatusRow> data = analyticsService.purchasesByStatus(
                appId, from(from), to(to), itemId, method(paymentMethod));
        return ApiResponse.ok(data, RequestContext.getRequestId());
    }

    @GetMapping("/events")
    public ApiResponse<List<EventDto>> events(
            @PathVariable String appId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        authorize(appId);
        return ApiResponse.ok(analyticsService.events(appId, from(from), to(to)), RequestContext.getRequestId());
    }

    private void authorize(String appId) {
        appService.requireOwnedApp(appId, PortalContext.requireUser());
    }

    private Instant from(LocalDate from) {
        return DateRanges.resolveFrom(from);
    }

    private Instant to(LocalDate to) {
        return DateRanges.resolveTo(to);
    }

    private PaymentMethod method(String raw) {
        return PaymentMethods.parseOrNull(raw);
    }
}
