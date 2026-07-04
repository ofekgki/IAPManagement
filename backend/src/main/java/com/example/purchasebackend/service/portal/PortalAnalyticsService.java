package com.example.purchasebackend.service.portal;

import com.example.purchasebackend.domain.AnalyticsEvent;
import com.example.purchasebackend.domain.Purchase;
import com.example.purchasebackend.domain.PurchaseItem;
import com.example.purchasebackend.domain.enums.EntitlementStatus;
import com.example.purchasebackend.domain.enums.PaymentMethod;
import com.example.purchasebackend.domain.enums.PurchaseStatus;
import com.example.purchasebackend.dto.portal.AnalyticsDtos.EventDto;
import com.example.purchasebackend.dto.portal.AnalyticsDtos.FunnelResponse;
import com.example.purchasebackend.dto.portal.AnalyticsDtos.FunnelStep;
import com.example.purchasebackend.dto.portal.AnalyticsDtos.OverviewResponse;
import com.example.purchasebackend.dto.portal.AnalyticsDtos.PurchaseStatusRow;
import com.example.purchasebackend.dto.portal.AnalyticsDtos.RevenueByPaymentMethodRow;
import com.example.purchasebackend.dto.portal.AnalyticsDtos.RevenueByProductRow;
import com.example.purchasebackend.dto.portal.AnalyticsDtos.RevenueByTimePoint;
import com.example.purchasebackend.dto.portal.AnalyticsDtos.RevenueSummaryResponse;
import com.example.purchasebackend.repository.AnalyticsEventRepository;
import com.example.purchasebackend.repository.EntitlementRepository;
import com.example.purchasebackend.repository.PurchaseItemRepository;
import com.example.purchasebackend.repository.PurchaseRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.IsoFields;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Computes the portal's analytics and revenue from backend data only (no fake numbers in production).
 *
 * <p>Revenue is counted from SUCCESS purchases only, using {@link PurchaseItem#getPriceAmountMinor()}
 * (never the display string). All conversion ratios are division-by-zero safe.
 */
@Service
public class PortalAnalyticsService {

    private final AnalyticsEventRepository eventRepository;
    private final PurchaseRepository purchaseRepository;
    private final PurchaseItemRepository itemRepository;
    private final EntitlementRepository entitlementRepository;

    public PortalAnalyticsService(AnalyticsEventRepository eventRepository,
                                  PurchaseRepository purchaseRepository,
                                  PurchaseItemRepository itemRepository,
                                  EntitlementRepository entitlementRepository) {
        this.eventRepository = eventRepository;
        this.purchaseRepository = purchaseRepository;
        this.itemRepository = itemRepository;
        this.entitlementRepository = entitlementRepository;
    }

    // --- overview ----------------------------------------------------------------------------

    public OverviewResponse overview(String appId, Instant from, Instant to, String itemId,
                                     PaymentMethod paymentMethod) {
        EventCounts counts = eventCounts(appId, from, to, itemId);
        List<Purchase> successes = successfulPurchases(appId, from, to, itemId, paymentMethod);
        Map<String, PurchaseItem> items = itemsById(appId);

        long popupShown = counts.of("purchase_popup_shown");
        long cancelClicked = counts.of("purchase_cancel_clicked");
        long confirmClicked = counts.of("purchase_confirm_clicked");
        long started = counts.of("purchase_started");
        long success = counts.of("purchase_success");
        long failed = counts.of("purchase_failed");
        long restoreStarted = counts.of("restore_started");
        long restoreSuccess = counts.of("restore_success");
        long restoreFailed = counts.of("restore_failed");
        long entitlementChecked = counts.of("entitlement_checked");

        long grossRevenue = totalRevenueMinor(successes, items);
        // Restores are refunds: subtract their item prices from total revenue.
        long refunded = totalRevenueMinor(restoredPurchases(appId, from, to, itemId, paymentMethod), items);
        long totalRevenue = grossRevenue - refunded;
        long avgRevenue = successes.isEmpty() ? 0 : grossRevenue / successes.size();
        long activeEntitlements = entitlementRepository.countByDeveloperAppIdAndStatus(appId, EntitlementStatus.ACTIVE);

        return new OverviewResponse(
                popupShown, cancelClicked, confirmClicked, started, success, failed,
                restoreStarted, restoreSuccess, restoreFailed, entitlementChecked,
                safeRatio(success, popupShown),
                safeRatio(success, confirmClicked),
                safeRatio(cancelClicked, popupShown),
                totalRevenue, avgRevenue, primaryCurrency(items),
                activeEntitlements, restoreStarted);
    }

    // --- funnel ------------------------------------------------------------------------------

    public FunnelResponse funnel(String appId, Instant from, Instant to, String itemId,
                                 PaymentMethod paymentMethod) {
        // The funnel is built from analytics events, which don't carry a payment method; the
        // paymentMethod filter therefore applies only to revenue/purchase metrics, not the funnel.
        EventCounts counts = eventCounts(appId, from, to, itemId);
        long popupShown = counts.of("purchase_popup_shown");
        long confirmClicked = counts.of("purchase_confirm_clicked");
        long started = counts.of("purchase_started");
        long success = counts.of("purchase_success");
        long cancelClicked = counts.of("purchase_cancel_clicked");

        List<FunnelStep> main = List.of(
                step("purchase_popup_shown", popupShown, popupShown, popupShown),
                step("purchase_confirm_clicked", confirmClicked, popupShown, popupShown),
                step("purchase_started", started, popupShown, confirmClicked),
                step("purchase_success", success, popupShown, started));
        FunnelStep cancelBranch = step("purchase_cancel_clicked", cancelClicked, popupShown, popupShown);
        return new FunnelResponse(main, cancelBranch, popupShown);
    }

    // --- revenue summary ---------------------------------------------------------------------

    public RevenueSummaryResponse revenue(String appId, Instant from, Instant to, String itemId,
                                          PaymentMethod paymentMethod, String groupBy) {
        List<Purchase> successes = successfulPurchases(appId, from, to, itemId, paymentMethod);
        // Restores are refunds: their item price is subtracted from revenue everywhere.
        List<Purchase> restored = restoredPurchases(appId, from, to, itemId, paymentMethod);
        Map<String, PurchaseItem> items = itemsById(appId);

        Map<String, long[]> byMethod = new LinkedHashMap<>(); // payment method -> [revenue, count]
        for (Purchase p : successes) {
            String key = p.getPaymentMethod() == null ? "UNKNOWN" : p.getPaymentMethod().name();
            byMethod.computeIfAbsent(key, k -> new long[2]);
            byMethod.get(key)[0] += priceOf(p, items);
            byMethod.get(key)[1] += 1;
        }
        for (Purchase p : restored) { // refund from the method's revenue (count stays gross)
            String key = p.getPaymentMethod() == null ? "UNKNOWN" : p.getPaymentMethod().name();
            byMethod.computeIfAbsent(key, k -> new long[2]);
            byMethod.get(key)[0] -= priceOf(p, items);
        }
        List<RevenueByPaymentMethodRow> methodRows = byMethod.entrySet().stream()
                .map(e -> new RevenueByPaymentMethodRow(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();

        long netTotal = totalRevenueMinor(successes, items) - totalRevenueMinor(restored, items);
        return new RevenueSummaryResponse(
                netTotal,
                primaryCurrency(items),
                methodRows,
                bucketByTime(successes, restored, items, groupBy, from, to),
                totalRevenueMinor(restored, items),
                restored.size());
    }

    public List<RevenueByProductRow> revenueByProduct(String appId, Instant from, Instant to,
                                                      PaymentMethod paymentMethod) {
        List<Purchase> successes = successfulPurchases(appId, from, to, null, paymentMethod);
        List<Purchase> restored = restoredPurchases(appId, from, to, null, paymentMethod);
        List<AnalyticsEvent> events = filteredEvents(appId, from, to, null);
        Map<String, PurchaseItem> items = itemsById(appId);

        return items.values().stream().map(item -> {
            long sold = successes.stream().filter(p -> p.getItemId().equals(item.getItemId())).count();
            long returned = restored.stream().filter(p -> p.getItemId().equals(item.getItemId())).count();
            // Net units after refunds/restores (never below 0 for display).
            long count = Math.max(0, sold - returned);
            long price = item.getPriceAmountMinor() == null ? 0 : item.getPriceAmountMinor();
            long revenue = count * price;
            long popupViews = events.stream()
                    .filter(e -> "purchase_popup_shown".equals(e.getEventName())
                            && item.getItemId().equals(e.getItemId()))
                    .count();
            return new RevenueByProductRow(
                    item.getItemId(), item.getName(), count, revenue,
                    count == 0 ? 0 : revenue / count, popupViews,
                    safeRatio(count, popupViews), item.getCurrency());
        }).toList();
    }

    public List<RevenueByTimePoint> revenueByTime(String appId, Instant from, Instant to,
                                                  String itemId, PaymentMethod paymentMethod, String groupBy) {
        List<Purchase> successes = successfulPurchases(appId, from, to, itemId, paymentMethod);
        List<Purchase> restored = restoredPurchases(appId, from, to, itemId, paymentMethod);
        return bucketByTime(successes, restored, itemsById(appId), groupBy, from, to);
    }

    public List<EventDto> events(String appId, Instant from, Instant to) {
        return eventRepository
                .findTop200ByDeveloperAppIdAndCreatedAtBetweenOrderByCreatedAtDesc(appId, from, to)
                .stream()
                .map(e -> new EventDto(e.getId(), e.getUserId(), e.getEventName(),
                        e.getBillingMode() == null ? null : e.getBillingMode().name(),
                        e.getItemId(), e.getPurchaseId(), e.getCreatedAt(), e.getMetadataJson()))
                .toList();
    }

    /**
     * Counts purchases grouped by {@link PurchaseStatus} (SUCCESS / FAILED / CANCELLED / PENDING / …)
     * from the purchase table — the authoritative record — within the date range. Only non-zero
     * statuses are returned, in enum order.
     */
    public List<PurchaseStatusRow> purchasesByStatus(String appId, Instant from, Instant to,
                                                     String itemId, PaymentMethod paymentMethod) {
        Map<PurchaseStatus, Long> counts = new java.util.EnumMap<>(PurchaseStatus.class);
        // Window pushed down to the DB (idx_purchase_app_created); item/method filtered in memory.
        purchaseRepository
                .findByDeveloperAppIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                        appId, from, to)
                .stream()
                .filter(p -> itemId == null || itemId.equals(p.getItemId()))
                .filter(p -> paymentMethod == null || paymentMethod == p.getPaymentMethod())
                .forEach(p -> counts.merge(p.getStatus(), 1L, Long::sum));

        List<PurchaseStatusRow> rows = new java.util.ArrayList<>();
        for (PurchaseStatus status : PurchaseStatus.values()) {
            long count = counts.getOrDefault(status, 0L);
            if (count > 0) {
                rows.add(new PurchaseStatusRow(status.name(), count));
            }
        }
        return rows;
    }

    // --- helpers -----------------------------------------------------------------------------

    private List<AnalyticsEvent> filteredEvents(String appId, Instant from, Instant to, String itemId) {
        return eventRepository.findByDeveloperAppIdAndCreatedAtBetween(appId, from, to).stream()
                .filter(e -> itemId == null || itemId.equals(e.getItemId()))
                .toList();
    }

    /** Counts of events by name within a window, resolved by the cheapest available strategy. */
    @FunctionalInterface
    private interface EventCounts {
        long of(String eventName);
    }

    /**
     * Unfiltered requests (the common portal case) count each event name with an index-only COUNT
     * query on idx_evt_app_name_time — no entities are materialized. With an item filter the
     * window's events are fetched once and counted in memory (the filter isn't indexed).
     */
    private EventCounts eventCounts(String appId, Instant from, Instant to, String itemId) {
        if (itemId == null) {
            return name -> eventRepository.countByDeveloperAppIdAndEventNameAndCreatedAtBetween(
                    appId, name, from, to);
        }
        List<AnalyticsEvent> events = filteredEvents(appId, from, to, itemId);
        return name -> countEvent(events, name);
    }

    private List<Purchase> successfulPurchases(String appId, Instant from, Instant to,
                                               String itemId, PaymentMethod paymentMethod) {
        return purchasesInWindow(appId, from, to, itemId, paymentMethod, PurchaseStatus.SUCCESS);
    }

    /** Restores are treated as refunds: their item price is SUBTRACTED from revenue. */
    private List<Purchase> restoredPurchases(String appId, Instant from, Instant to,
                                             String itemId, PaymentMethod paymentMethod) {
        return purchasesInWindow(appId, from, to, itemId, paymentMethod, PurchaseStatus.RESTORED);
    }

    /**
     * Purchases in a given status whose completion falls in the [from, to) window, with filters.
     * The window is pushed down to the DB (idx_purchase_app_status_completed) instead of loading
     * the app's full history. Only called for SUCCESS/RESTORED, which always set completedAt.
     */
    private List<Purchase> purchasesInWindow(String appId, Instant from, Instant to,
                                             String itemId, PaymentMethod paymentMethod, PurchaseStatus status) {
        return purchaseRepository
                .findByDeveloperAppIdAndStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(
                        appId, status, from, to)
                .stream()
                .filter(p -> itemId == null || itemId.equals(p.getItemId()))
                .filter(p -> paymentMethod == null || paymentMethod == p.getPaymentMethod())
                .toList();
    }

    private Map<String, PurchaseItem> itemsById(String appId) {
        Map<String, PurchaseItem> map = new LinkedHashMap<>();
        for (PurchaseItem item : itemRepository.findByDeveloperAppId(appId)) {
            map.put(item.getItemId(), item);
        }
        return map;
    }

    private long totalRevenueMinor(List<Purchase> successes, Map<String, PurchaseItem> items) {
        return successes.stream().mapToLong(p -> priceOf(p, items)).sum();
    }

    private long priceOf(Purchase purchase, Map<String, PurchaseItem> items) {
        // Prefer the price snapshot taken at purchase time (immune to later item-price edits);
        // fall back to the item's current list price for legacy rows without a snapshot.
        if (purchase.getPriceAmountMinor() != null) {
            return purchase.getPriceAmountMinor();
        }
        PurchaseItem item = items.get(purchase.getItemId());
        return item == null || item.getPriceAmountMinor() == null ? 0 : item.getPriceAmountMinor();
    }

    private List<RevenueByTimePoint> bucketByTime(List<Purchase> successes, List<Purchase> restored,
                                                  Map<String, PurchaseItem> items, String groupBy,
                                                  Instant from, Instant to) {
        Map<String, long[]> buckets = new TreeMap<>(); // bucket -> [revenue, count]
        // Pre-seed every period in the window with zeros so the chart shows an actual value on days
        // that had purchases and drops to 0 on days that didn't (instead of interpolating a straight
        // line between sparse points).
        prefillBuckets(buckets, from, to, groupBy);
        for (Purchase p : successes) {
            Instant when = p.getCompletedAt() != null ? p.getCompletedAt() : p.getCreatedAt();
            String bucket = bucketLabel(LocalDate.ofInstant(when, ZoneOffset.UTC), groupBy);
            buckets.computeIfAbsent(bucket, k -> new long[2]);
            buckets.get(bucket)[0] += priceOf(p, items);
            buckets.get(bucket)[1] += 1;
        }
        // Refund restores from their bucket's revenue (count is left as gross sales).
        for (Purchase p : restored) {
            Instant when = p.getCompletedAt() != null ? p.getCompletedAt() : p.getCreatedAt();
            String bucket = bucketLabel(LocalDate.ofInstant(when, ZoneOffset.UTC), groupBy);
            buckets.computeIfAbsent(bucket, k -> new long[2]);
            buckets.get(bucket)[0] -= priceOf(p, items);
        }
        return buckets.entrySet().stream()
                .map(e -> new RevenueByTimePoint(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();
    }

    /**
     * Seeds {@code buckets} with a zero entry for every day/week/month between {@code from} and
     * {@code to} so empty periods render as 0 rather than being skipped (which would make the chart
     * connect sparse points with a misleading straight line). Capped so a pathological range can't
     * generate an unbounded series.
     */
    private void prefillBuckets(Map<String, long[]> buckets, Instant from, Instant to, String groupBy) {
        if (from == null || to == null || !from.isBefore(to)) {
            return;
        }
        LocalDate day = LocalDate.ofInstant(from, ZoneOffset.UTC);
        // `to` is an exclusive upper bound; the last included day is the day just before it.
        LocalDate last = LocalDate.ofInstant(to.minusSeconds(1), ZoneOffset.UTC);
        int guard = 0;
        while (!day.isAfter(last) && guard++ < 1500) {
            buckets.computeIfAbsent(bucketLabel(day, groupBy), k -> new long[2]);
            day = day.plusDays(1);
        }
    }

    private String bucketLabel(LocalDate date, String groupBy) {
        String g = groupBy == null ? "day" : groupBy.toLowerCase();
        return switch (g) {
            case "week" -> date.get(IsoFields.WEEK_BASED_YEAR) + "-W"
                    + String.format("%02d", date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
            case "month" -> String.format("%04d-%02d", date.getYear(), date.getMonthValue());
            default -> date.toString(); // ISO yyyy-MM-dd
        };
    }

    private FunnelStep step(String name, long count, long top, long prev) {
        return new FunnelStep(name, count, safeRatio(count, top), safeRatio(count, prev));
    }

    private long countEvent(List<AnalyticsEvent> events, String name) {
        return events.stream().filter(e -> name.equals(e.getEventName())).count();
    }

    private String primaryCurrency(Map<String, PurchaseItem> items) {
        return items.values().stream()
                .map(PurchaseItem::getCurrency)
                .filter(c -> c != null && !c.isBlank())
                .findFirst().orElse("USD");
    }

    /** Division-by-zero safe ratio in [0,1]. */
    private double safeRatio(long numerator, long denominator) {
        return denominator <= 0 ? 0.0 : (double) numerator / (double) denominator;
    }
}
