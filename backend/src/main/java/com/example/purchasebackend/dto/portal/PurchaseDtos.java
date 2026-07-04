package com.example.purchasebackend.dto.portal;

import java.time.Instant;
import java.util.List;

/** Portal purchase DTOs. Provider purchase tokens are never exposed. */
public final class PurchaseDtos {

    private PurchaseDtos() {
    }

    public record PortalPurchaseDto(
            String purchaseId,
            String userId,
            String itemId,
            String billingMode,
            String paymentMethod,
            String status,
            Long revenueMinor,
            // The item's list price. Shown for RESTORED purchases (which carry 0 revenue) so the
            // portal's purchase-detail view can still display the item's original price.
            Long originalPriceMinor,
            String currency,
            String provider,
            String failureCode,
            Instant createdAt,
            Instant completedAt) {
    }

    /** One page of the purchases listing, so the portal never downloads the full history at once. */
    public record PagedPurchasesDto(
            List<PortalPurchaseDto> items,
            int page,
            int size,
            long totalItems,
            int totalPages) {
    }

    public record PurchaseEventDto(String eventName, String billingMode, Instant createdAt) {
    }

    public record GrantedEntitlementDto(String entitlementId, String status, Instant expiresAt) {
    }

    public record PurchaseDetailDto(
            PortalPurchaseDto purchase,
            String itemName,
            String itemType,
            String failureMessage,
            // TODO: Store only shortened or hashed provider purchase tokens in logs.
            String providerOrderId,
            GrantedEntitlementDto entitlement,
            List<PurchaseEventDto> events) {
    }
}
