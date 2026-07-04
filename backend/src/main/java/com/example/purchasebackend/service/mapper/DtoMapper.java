package com.example.purchasebackend.service.mapper;

import com.example.purchasebackend.domain.Entitlement;
import com.example.purchasebackend.domain.Purchase;
import com.example.purchasebackend.domain.PurchaseItem;
import com.example.purchasebackend.dto.internal.AdminItemDto;
import com.example.purchasebackend.dto.internal.AdminPurchaseDto;
import com.example.purchasebackend.dto.sdk.EntitlementDto;
import com.example.purchasebackend.dto.sdk.EntitlementSummaryDto;
import com.example.purchasebackend.dto.sdk.ItemDto;

/** Entity → DTO conversions. Keeps internal persistence models out of API responses. */
public final class DtoMapper {

    private DtoMapper() {
    }

    public static ItemDto toItemDto(PurchaseItem item) {
        return new ItemDto(
                item.getItemId(),
                item.getName(),
                item.getDescription(),
                item.getType().name(),
                item.getPriceDisplay(),
                item.getCurrency(),
                item.getEntitlementId());
    }

    public static AdminItemDto toAdminItemDto(PurchaseItem item) {
        return new AdminItemDto(
                item.getId(),
                item.getDeveloperAppId(),
                item.getItemId(),
                item.getName(),
                item.getDescription(),
                item.getType().name(),
                item.getPriceDisplay(),
                item.getCurrency(),
                item.getGooglePlayProductId(),
                item.getEntitlementId(),
                item.isActive(),
                item.getCreatedAt(),
                item.getUpdatedAt());
    }

    public static EntitlementSummaryDto toEntitlementSummary(Entitlement e) {
        return new EntitlementSummaryDto(e.getEntitlementId(), e.getStatus().name(), e.getExpiresAt());
    }

    public static EntitlementDto toEntitlementDto(Entitlement e) {
        return new EntitlementDto(
                e.getEntitlementId(), e.getSourceItemId(), e.getStatus().name(), e.getExpiresAt());
    }

    public static AdminPurchaseDto toAdminPurchaseDto(Purchase p) {
        return new AdminPurchaseDto(
                p.getId(),
                p.getDeveloperAppId(),
                p.getUserId(),
                p.getItemId(),
                p.getBillingMode().name(),
                p.getStatus().name(),
                p.getProvider().name(),
                p.getProviderOrderId(),
                p.getIdempotencyKey(),
                p.getFailureCode(),
                p.getCreatedAt(),
                p.getCompletedAt());
    }
}
