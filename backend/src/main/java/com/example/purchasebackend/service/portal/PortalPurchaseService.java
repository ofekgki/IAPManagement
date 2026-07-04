package com.example.purchasebackend.service.portal;

import com.example.purchasebackend.common.ApiException;
import com.example.purchasebackend.common.ErrorCode;
import com.example.purchasebackend.domain.Entitlement;
import com.example.purchasebackend.domain.Purchase;
import com.example.purchasebackend.domain.PurchaseItem;
import com.example.purchasebackend.domain.enums.PaymentMethod;
import com.example.purchasebackend.domain.enums.PurchaseStatus;
import com.example.purchasebackend.dto.portal.PurchaseDtos.GrantedEntitlementDto;
import com.example.purchasebackend.dto.portal.PurchaseDtos.PagedPurchasesDto;
import com.example.purchasebackend.dto.portal.PurchaseDtos.PortalPurchaseDto;
import com.example.purchasebackend.dto.portal.PurchaseDtos.PurchaseDetailDto;
import com.example.purchasebackend.dto.portal.PurchaseDtos.PurchaseEventDto;
import com.example.purchasebackend.repository.AnalyticsEventRepository;
import com.example.purchasebackend.repository.EntitlementRepository;
import com.example.purchasebackend.repository.PurchaseItemRepository;
import com.example.purchasebackend.repository.PurchaseRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Portal purchase listing + detail. Provider purchase tokens are never exposed. */
@Service
public class PortalPurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final PurchaseItemRepository itemRepository;
    private final EntitlementRepository entitlementRepository;
    private final AnalyticsEventRepository eventRepository;

    public PortalPurchaseService(PurchaseRepository purchaseRepository,
                                 PurchaseItemRepository itemRepository,
                                 EntitlementRepository entitlementRepository,
                                 AnalyticsEventRepository eventRepository) {
        this.purchaseRepository = purchaseRepository;
        this.itemRepository = itemRepository;
        this.entitlementRepository = entitlementRepository;
        this.eventRepository = eventRepository;
    }

    /**
     * One page of purchases, newest first. The date window is pushed down to the DB
     * (idx_purchase_app_created, already ordered) and the optional filters are applied in memory;
     * only the requested page of DTOs is returned to the client.
     */
    public PagedPurchasesDto list(String appId, Instant from, Instant to, PurchaseStatus status,
                                  String itemId, PaymentMethod paymentMethod, String userId,
                                  int page, int size) {
        Map<String, PurchaseItem> items = itemsById(appId);
        // userId/itemId are typed by hand in the portal, so match them as case-insensitive substrings
        // ("contains") rather than exact equality — typing a partial or differently-cased id still hits.
        String userNeedle = normalize(userId);
        String itemNeedle = normalize(itemId);
        List<Purchase> filtered = purchaseRepository
                .findByDeveloperAppIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                        appId, from, to)
                .stream()
                .filter(p -> status == null || p.getStatus() == status)
                .filter(p -> itemNeedle == null || contains(p.getItemId(), itemNeedle))
                .filter(p -> paymentMethod == null || paymentMethod == p.getPaymentMethod())
                .filter(p -> userNeedle == null || contains(p.getUserId(), userNeedle))
                .toList();

        long totalItems = filtered.size();
        int totalPages = (int) Math.max(1, (totalItems + size - 1) / size);
        int safePage = Math.min(Math.max(0, page), totalPages - 1);
        List<PortalPurchaseDto> pageItems = filtered.stream()
                .skip((long) safePage * size)
                .limit(size)
                .map(p -> toDto(p, items))
                .toList();
        return new PagedPurchasesDto(pageItems, safePage, size, totalItems, totalPages);
    }

    public PurchaseDetailDto detail(String appId, String purchaseId) {
        Purchase purchase = purchaseRepository.findByIdAndDeveloperAppId(purchaseId, appId)
                .orElseThrow(() -> new ApiException(ErrorCode.PURCHASE_NOT_FOUND));
        Map<String, PurchaseItem> items = itemsById(appId);
        PurchaseItem item = items.get(purchase.getItemId());

        GrantedEntitlementDto entitlement = null;
        if (item != null && item.getEntitlementId() != null) {
            Entitlement e = entitlementRepository
                    .findFirstByDeveloperAppIdAndUserIdAndEntitlementIdOrderByCreatedAtDesc(
                            appId, purchase.getUserId(), item.getEntitlementId())
                    .orElse(null);
            if (e != null) {
                entitlement = new GrantedEntitlementDto(e.getEntitlementId(), e.getStatus().name(), e.getExpiresAt());
            }
        }

        List<PurchaseEventDto> events = eventRepository.findByPurchaseId(purchaseId).stream()
                .map(e -> new PurchaseEventDto(e.getEventName(),
                        e.getBillingMode() == null ? null : e.getBillingMode().name(), e.getCreatedAt()))
                .toList();

        return new PurchaseDetailDto(
                toDto(purchase, items),
                item == null ? null : item.getName(),
                item == null ? null : item.getType().name(),
                purchase.getFailureMessage(),
                // Provider order id is safe to show; the raw purchase token is intentionally omitted.
                purchase.getProviderOrderId(),
                entitlement,
                events);
    }

    private PortalPurchaseDto toDto(Purchase p, Map<String, PurchaseItem> items) {
        PurchaseItem item = items.get(p.getItemId());
        // Prefer the price snapshot taken at purchase time (immune to later item-price edits);
        // fall back to the item's current list price for legacy rows without one.
        String currency = p.getPriceCurrency() != null ? p.getPriceCurrency()
                : (item == null ? null : item.getCurrency());
        Long originalPrice = p.getPriceAmountMinor() != null ? p.getPriceAmountMinor()
                : (item == null ? null : item.getPriceAmountMinor());
        // Revenue counts only SUCCESS sales; a RESTORED purchase carries 0 revenue but still exposes
        // the price paid via originalPriceMinor for the purchase-detail view.
        Long revenue = (p.getStatus() == PurchaseStatus.SUCCESS) ? originalPrice : null;
        return new PortalPurchaseDto(
                p.getId(), p.getUserId(), p.getItemId(),
                p.getBillingMode() == null ? null : p.getBillingMode().name(),
                p.getPaymentMethod() == null ? null : p.getPaymentMethod().name(),
                p.getStatus().name(),
                revenue, originalPrice, currency,
                p.getProvider() == null ? null : p.getProvider().name(), p.getFailureCode(),
                p.getCreatedAt(), p.getCompletedAt());
    }

    /** Lower-cased, trimmed filter needle, or null when blank. */
    private static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase();
    }

    /** Case-insensitive "contains" against an already-lower-cased [needle]. */
    private static boolean contains(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase().contains(needle);
    }

    private Map<String, PurchaseItem> itemsById(String appId) {
        return itemRepository.findByDeveloperAppId(appId).stream()
                .collect(Collectors.toMap(PurchaseItem::getItemId, Function.identity(), (a, b) -> a));
    }

}
