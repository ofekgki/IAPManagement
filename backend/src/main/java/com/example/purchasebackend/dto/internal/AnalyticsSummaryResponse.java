package com.example.purchasebackend.dto.internal;

/** Basic event count aggregation for GET /api/v1/internal/analytics/summary. */
public record AnalyticsSummaryResponse(
        long purchaseStarted,
        long purchaseSuccess,
        long purchaseFailed,
        long popupShown,
        long restoreStarted
) {
}
