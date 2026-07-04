package com.example.purchasebackend.dto.portal;

import java.time.Instant;
import java.util.List;

/** Portal analytics + revenue DTOs. All conversion ratios are 0..1 and division-by-zero safe. */
public final class AnalyticsDtos {

    private AnalyticsDtos() {
    }

    /** KPI cards for the analytics dashboard. */
    public record OverviewResponse(
            long popupShown,
            long cancelClicked,
            long confirmClicked,
            long purchaseStarted,
            long purchaseSuccess,
            long purchaseFailed,
            long restoreStarted,
            long restoreSuccess,
            long restoreFailed,
            long entitlementChecked,
            double popupToSuccessConversion,
            double confirmToSuccessConversion,
            double cancelRate,
            long totalRevenueMinor,
            long averageRevenueMinor,
            String currency,
            long activeEntitlements,
            long restorePurchasesCount) {
    }

    public record FunnelStep(String step, long count, double percentOfTop, double percentOfPrev) {
    }

    public record FunnelResponse(List<FunnelStep> mainFunnel, FunnelStep cancelBranch, long popupShown) {
    }

    public record RevenueByPaymentMethodRow(String paymentMethod, long revenueMinor, long purchases) {
    }

    public record RevenueByTimePoint(String bucket, long revenueMinor, long purchases) {
    }

    public record RevenueSummaryResponse(
            long totalRevenueMinor,
            String currency,
            List<RevenueByPaymentMethodRow> byPaymentMethod,
            List<RevenueByTimePoint> overTime,
            // Restores are NOT sales, so they never touch totalRevenueMinor. These expose the value that
            // was restored (sum of the restored items' original prices) as a separate, informational
            // metric so restores are visible without corrupting revenue.
            long restoredValueMinor,
            long restoredCount) {
    }

    public record RevenueByProductRow(
            String itemId,
            String name,
            long successfulPurchases,
            long totalRevenueMinor,
            long averageRevenueMinor,
            long popupViews,
            double conversionRate,
            String currency) {
    }

    /** One row of the purchases-by-status breakdown (from the purchase table, not events). */
    public record PurchaseStatusRow(String status, long count) {
    }

    public record EventDto(
            String id,
            String userId,
            String eventName,
            String billingMode,
            String itemId,
            String purchaseId,
            Instant createdAt,
            String metadataJson) {
    }
}
