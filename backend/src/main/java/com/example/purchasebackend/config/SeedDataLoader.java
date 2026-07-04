package com.example.purchasebackend.config;

import com.example.purchasebackend.common.Ids;
import com.example.purchasebackend.domain.AnalyticsEvent;
import com.example.purchasebackend.domain.ApiKey;
import com.example.purchasebackend.domain.DeveloperApp;
import com.example.purchasebackend.domain.DeveloperUser;
import com.example.purchasebackend.domain.Entitlement;
import com.example.purchasebackend.domain.Purchase;
import com.example.purchasebackend.domain.PurchaseItem;
import com.example.purchasebackend.domain.enums.ApiKeyStatus;
import com.example.purchasebackend.domain.enums.BillingMode;
import com.example.purchasebackend.domain.enums.BillingProviderType;
import com.example.purchasebackend.domain.enums.DeveloperUserRole;
import com.example.purchasebackend.domain.enums.EntitlementStatus;
import com.example.purchasebackend.domain.enums.ItemType;
import com.example.purchasebackend.domain.enums.PaymentMethod;
import com.example.purchasebackend.domain.enums.PurchaseStatus;
import com.example.purchasebackend.repository.AnalyticsEventRepository;
import com.example.purchasebackend.repository.ApiKeyRepository;
import com.example.purchasebackend.repository.DeveloperAppRepository;
import com.example.purchasebackend.repository.DeveloperUserRepository;
import com.example.purchasebackend.repository.EntitlementRepository;
import com.example.purchasebackend.repository.IdempotencyRecordRepository;
import com.example.purchasebackend.repository.PurchaseItemRepository;
import com.example.purchasebackend.repository.PurchaseRepository;
import com.example.purchasebackend.security.PasswordHasher;
import com.example.purchasebackend.service.ApiKeyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Demo seed data for local development / the demo stack (gated by {@code app.seed.enabled=true}, set
 * in the {@code dev} and {@code docker} profiles). It creates the demo portal user, the demo app, an
 * API key, a rich item catalog, and — when {@code app.seed.sample-data=true} — a realistic history of
 * purchases, entitlements, and analytics events spread from {@code app.seed.start-date} (default
 * 2026-01-01) to {@code app.seed.end-date} (default: today) so the portal dashboards look real.
 *
 * <h2>Why the data is shaped, not random</h2>
 * Volume follows a monthly story (January low → March campaign spike → June yearly push) and a small
 * weekend bump; cheap consumables convert better than expensive subscriptions; subscriptions started
 * long ago are already EXPIRED while recent ones are ACTIVE. This makes every portal chart
 * (revenue-over-time, conversion, top items, active-vs-expired) meaningful instead of flat.
 *
 * <h2>Idempotency & reset</h2>
 * {@link #run} seeds only if the demo app is absent, so restarts don't duplicate data. The portal can
 * call {@link #reset(boolean)} (via {@code PortalMaintenanceController}) to wipe everything and
 * optionally regenerate from scratch.
 *
 * <p>Production never seeds (the property is unset there).
 *
 * <p>// TODO: Replace demo seed data with real developer-portal item creation + real purchases.
 * <p>// TODO: Multi-currency support — all demo prices use a single currency (USD) so revenue sums
 *      stay meaningful. Real revenue analytics must convert per-currency before summing.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class SeedDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataLoader.class);

    private static final String DEMO_USER_EMAIL = "demo@example.com";
    private static final String DEMO_USER_PASSWORD = "password123";
    private static final String DEMO_APP_ID = "app_demo";
    private static final String DEMO_API_KEY = "demo_api_key_123"; // dev only — never ship a real key
    private static final String CURRENCY = "USD";

    /** The default end-user the demo Android app signs in as (see PurchaseSdk.init in the demo app). */
    private static final String DEMO_END_USER_ID = "demo-user-001";

    // Generator tuning. Keep these modest so an in-memory H2 seed stays fast.
    private static final int BASE_DAILY_ATTEMPTS = 6; // before monthly/weekend multipliers
    private static final double CANCEL_RATE = 0.18;
    private static final double FAIL_RATE = 0.07;
    private static final int USER_POOL_SIZE = 40;
    private static final long RANDOM_SEED = 20260101L; // deterministic → reruns produce identical data

    private final DeveloperUserRepository userRepository;
    private final DeveloperAppRepository appRepository;
    private final PurchaseItemRepository itemRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final PurchaseRepository purchaseRepository;
    private final EntitlementRepository entitlementRepository;
    private final AnalyticsEventRepository eventRepository;
    private final IdempotencyRecordRepository idempotencyRepository;
    private final PasswordHasher passwordHasher;
    private final ApiKeyService apiKeyService;
    private final boolean sampleData;
    private final String startDateStr;
    private final String endDateStr;

    public SeedDataLoader(DeveloperUserRepository userRepository,
                          DeveloperAppRepository appRepository,
                          PurchaseItemRepository itemRepository,
                          ApiKeyRepository apiKeyRepository,
                          PurchaseRepository purchaseRepository,
                          EntitlementRepository entitlementRepository,
                          AnalyticsEventRepository eventRepository,
                          IdempotencyRecordRepository idempotencyRepository,
                          PasswordHasher passwordHasher,
                          ApiKeyService apiKeyService,
                          @Value("${app.seed.sample-data:true}") boolean sampleData,
                          @Value("${app.seed.start-date:2026-01-01}") String startDateStr,
                          @Value("${app.seed.end-date:}") String endDateStr) {
        this.userRepository = userRepository;
        this.appRepository = appRepository;
        this.itemRepository = itemRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.purchaseRepository = purchaseRepository;
        this.entitlementRepository = entitlementRepository;
        this.eventRepository = eventRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.passwordHasher = passwordHasher;
        this.apiKeyService = apiKeyService;
        this.sampleData = sampleData;
        this.startDateStr = startDateStr;
        this.endDateStr = endDateStr;
    }

    @Override
    public void run(String... args) {
        if (appRepository.findById(DEMO_APP_ID).isPresent()) {
            // Already seeded. If it's an OLDER/smaller seed (e.g. a pre-upgrade 4-item catalog left in
            // a persistent Postgres volume), refresh it so the demo isn't stuck on stale data.
            long itemCount = itemRepository.findByDeveloperAppId(DEMO_APP_ID).size();
            if (itemCount < CATALOG.size()) {
                log.info("Demo catalog looks outdated ({} items, current catalog has {}); regenerating demo data.",
                        itemCount, CATALOG.size());
                reset(true);
            }
            return; // up to date — don't duplicate on restart
        }
        seedAll();
    }

    /**
     * Wipes all demo transactional data and (optionally) regenerates it. Portal-triggered.
     * Developer portal user accounts are intentionally preserved so the caller stays logged in.
     *
     * @param reseed when true, regenerate the full demo dataset after wiping.
     * @return a small summary of the resulting row counts.
     */
    public synchronized Map<String, Object> reset(boolean reseed) {
        wipeAll();
        if (reseed) {
            seedAll();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reseeded", reseed);
        result.put("items", itemRepository.count());
        result.put("purchases", purchaseRepository.count());
        result.put("entitlements", entitlementRepository.count());
        result.put("events", eventRepository.count());
        log.info("Demo data reset (reseed={}): items={}, purchases={}, entitlements={}, events={}",
                reseed, result.get("items"), result.get("purchases"),
                result.get("entitlements"), result.get("events"));
        return result;
    }

    /** Deletes all demo transactional data (keeps portal user accounts). Soft FKs → order-independent. */
    private void wipeAll() {
        eventRepository.deleteAllInBatch();
        entitlementRepository.deleteAllInBatch();
        idempotencyRepository.deleteAllInBatch();
        purchaseRepository.deleteAllInBatch();
        itemRepository.deleteAllInBatch();
        apiKeyRepository.deleteAllInBatch();
        appRepository.deleteAllInBatch();
    }

    // --- seeding -----------------------------------------------------------------------------

    private void seedAll() {
        DeveloperUser user = userRepository.findByEmailIgnoreCase(DEMO_USER_EMAIL).orElseGet(() -> {
            DeveloperUser u = new DeveloperUser();
            u.setId(Ids.newId("usr"));
            u.setEmail(DEMO_USER_EMAIL);
            u.setPasswordHash(passwordHasher.hash(DEMO_USER_PASSWORD));
            u.setDisplayName("Demo Developer");
            u.setRole(DeveloperUserRole.OWNER);
            return userRepository.save(u);
        });

        DeveloperApp app = new DeveloperApp();
        app.setId(DEMO_APP_ID);
        app.setOwnerUserId(user.getId());
        app.setAppName("Demo Game");
        app.setPackageName("com.example.demogame");
        app.setDescription("Seed demo app for the in-app purchase platform.");
        app.setBillingModeDefault(BillingMode.MOCK);
        app.setActive(true);
        appRepository.save(app);

        // Fixed demo API key (so the SDK can authenticate with "demo_api_key_123").
        ApiKey key = new ApiKey();
        key.setId(Ids.newId("key"));
        key.setDeveloperAppId(app.getId());
        key.setName("Demo SDK Key");
        key.setKeyPrefix("demo_api_key_");
        key.setKeyHash(apiKeyService.hashApiKey(DEMO_API_KEY));
        key.setStatus(ApiKeyStatus.ACTIVE);
        apiKeyRepository.save(key);

        for (SeedItem it : CATALOG) {
            seedItem(app.getId(), it);
        }

        if (sampleData) {
            generateHistory(app.getId());
        }

        log.info("Seeded demo user '{}' (password '{}'), app '{}', apiKey '{}', {} items{}",
                DEMO_USER_EMAIL, DEMO_USER_PASSWORD, DEMO_APP_ID, DEMO_API_KEY, CATALOG.size(),
                sampleData ? " + realistic purchase/analytics history" : "");
    }

    private void seedItem(String appId, SeedItem it) {
        PurchaseItem item = new PurchaseItem();
        item.setId(Ids.newId("item"));
        item.setDeveloperAppId(appId);
        item.setItemId(it.itemId());
        item.setName(it.name());
        item.setDescription(it.description());
        item.setType(it.type());
        item.setEntitlementId(it.entitlementId());
        item.setPriceAmountMinor(it.priceMinor());
        item.setCurrency(CURRENCY);
        item.setPriceDisplay("$" + String.format("%.2f", it.priceMinor() / 100.0));
        item.setActive(true);
        itemRepository.save(item);
    }

    /**
     * Generates a realistic history of purchases + entitlements + analytics events across the
     * configured date window. DEV ONLY.
     */
    private void generateHistory(String appId) {
        LocalDate start = LocalDate.parse(startDateStr);
        LocalDate end = (endDateStr == null || endDateStr.isBlank())
                ? LocalDate.now(ZoneOffset.UTC) : LocalDate.parse(endDateStr);
        if (end.isBefore(start)) {
            end = start;
        }
        Instant now = Instant.now();
        Random rnd = new Random(RANDOM_SEED);

        List<String> users = new ArrayList<>();
        for (int i = 1; i <= USER_POOL_SIZE; i++) {
            users.add(String.format("user_%03d", i));
        }

        List<Purchase> purchases = new ArrayList<>();
        List<Entitlement> entitlements = new ArrayList<>();
        List<AnalyticsEvent> events = new ArrayList<>();

        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            // Base shape: the monthly story (campaigns) × a steady growth trend as the user base grows.
            long monthsIn = ChronoUnit.MONTHS.between(start.withDayOfMonth(1), day.withDayOfMonth(1));
            double multiplier = monthMultiplier(day.getMonthValue()) * (0.55 + 0.12 * monthsIn);
            if (day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY) {
                multiplier *= 1.3; // weekends are busier
            }
            // Day "personality": occasional sale/feature/viral spikes and the odd dead-quiet day, plus
            // wide daily noise — so the graphs look jagged and real, not a smooth curve.
            double dayRoll = rnd.nextDouble();
            boolean spikeDay = dayRoll > 0.94;        // ~6% of days: a sale / featured / viral spike
            boolean slowDay = dayRoll < 0.07;         // ~7% of days: unusually quiet
            if (spikeDay) {
                multiplier *= 2.2 + rnd.nextDouble() * 1.8; // ×2.2–4.0
            } else if (slowDay) {
                multiplier *= 0.3;
            }
            multiplier *= 0.6 + 0.8 * rnd.nextDouble(); // wide day-to-day variance
            int attempts = Math.max(0, (int) Math.round(BASE_DAILY_ATTEMPTS * multiplier));
            // On a spike day one item is "featured" (store placement / sale) and skews the mix, so the
            // top-selling item shifts over time instead of always being the same.
            SeedItem featured = spikeDay ? CATALOG.get(rnd.nextInt(CATALOG.size())) : null;

            for (int a = 0; a < attempts; a++) {
                SeedItem item = (featured != null && rnd.nextDouble() < 0.5)
                        ? featured : pickItem(rnd, day.getMonthValue());
                String user = users.get(rnd.nextInt(users.size()));
                Instant ts = instantOn(day, rnd, now);

                // Every attempt browses the item and opens the popup.
                events.add(event(appId, "item_viewed", item.itemId(), user, ts.minusSeconds(8), null));
                events.add(event(appId, "purchase_popup_shown", item.itemId(), user, ts, null));

                double roll = rnd.nextDouble();
                double successRate = successRate(item);
                if (roll < successRate) {
                    events.add(event(appId, "purchase_confirm_clicked", item.itemId(), user, ts.plusSeconds(3), null));
                    events.add(event(appId, "purchase_started", item.itemId(), user, ts.plusSeconds(4), null));
                    Purchase p = newPurchase(appId, item, user, PurchaseStatus.SUCCESS, ts.plusSeconds(5), rnd);
                    purchases.add(p);
                    events.add(event(appId, "purchase_success", item.itemId(), user, ts.plusSeconds(6), p.getId()));
                    if (item.entitlementId() != null) {
                        entitlements.add(newEntitlement(appId, item, user, p, ts.plusSeconds(5), now));
                    }
                } else if (roll < successRate + CANCEL_RATE) {
                    events.add(event(appId, "purchase_cancel_clicked", item.itemId(), user, ts.plusSeconds(3), null));
                    if (rnd.nextDouble() < 0.5) { // half of cancels leave a CANCELLED record
                        purchases.add(newPurchase(appId, item, user, PurchaseStatus.CANCELLED, ts.plusSeconds(3), rnd));
                    }
                } else if (roll < successRate + CANCEL_RATE + FAIL_RATE) {
                    events.add(event(appId, "purchase_confirm_clicked", item.itemId(), user, ts.plusSeconds(3), null));
                    events.add(event(appId, "purchase_started", item.itemId(), user, ts.plusSeconds(4), null));
                    Purchase p = newPurchase(appId, item, user, PurchaseStatus.FAILED, ts.plusSeconds(5), rnd);
                    p.setFailureCode("MOCK_DECLINED");
                    p.setFailureMessage("Simulated payment failure for demo data.");
                    purchases.add(p);
                    events.add(event(appId, "purchase_failed", item.itemId(), user, ts.plusSeconds(5), p.getId()));
                } else if (rnd.nextDouble() < 0.05) {
                    // A few attempts are left hanging as PENDING (started, never resolved).
                    purchases.add(newPurchase(appId, item, user, PurchaseStatus.PENDING, ts.plusSeconds(2), rnd));
                }
            }

            // App opens that check entitlements (e.g. "do I still have premium?").
            int checks = 2 + rnd.nextInt(5);
            for (int c = 0; c < checks; c++) {
                SeedItem item = pickItem(rnd, day.getMonthValue());
                String user = users.get(rnd.nextInt(users.size()));
                events.add(event(appId, "entitlement_checked", item.itemId(), user, instantOn(day, rnd, now), null));
            }

            // Some days a user taps "Restore purchases".
            if (rnd.nextDouble() < 0.4) {
                String user = users.get(rnd.nextInt(users.size()));
                Instant ts = instantOn(day, rnd, now);
                events.add(event(appId, "restore_started", null, user, ts, null));
                if (rnd.nextDouble() < 0.85) {
                    events.add(event(appId, "restore_success", null, user, ts.plusSeconds(1), null));
                } else {
                    events.add(event(appId, "restore_failed", null, user, ts.plusSeconds(1), null));
                }
            }
        }

        purchaseRepository.saveAll(purchases);
        entitlementRepository.saveAll(entitlements);
        eventRepository.saveAll(events);

        seedDemoUserOwnership(appId, now);

        log.info("Generated history {} → {}: {} purchases, {} entitlements, {} events",
                start, end, purchases.size(), entitlements.size(), events.size());
    }

    /**
     * Gives the default demo end-user ({@code demo-user-001}) a couple of recent, known entitlements
     * so the Android demo app immediately shows owned items, an active subscription, and restore data.
     */
    private void seedDemoUserOwnership(String appId, Instant now) {
        // Owned lifetime: Remove Ads (40 days ago).
        grantOwnership(appId, item("remove_ads"), now.minus(40, ChronoUnit.DAYS), now);
        // Active subscription: Pro Monthly (started 10 days ago, ~20 days left).
        grantOwnership(appId, item("pro_monthly"), now.minus(10, ChronoUnit.DAYS), now);
        // A consumable purchase (no permanent entitlement): 100 Coins (2 days ago).
        Purchase coins = newPurchase(appId, item("coins_100"), DEMO_END_USER_ID,
                PurchaseStatus.SUCCESS, now.minus(2, ChronoUnit.DAYS), new Random(7));
        purchaseRepository.save(coins);
    }

    private void grantOwnership(String appId, SeedItem item, Instant when, Instant now) {
        Purchase p = newPurchase(appId, item, DEMO_END_USER_ID, PurchaseStatus.SUCCESS, when, new Random(11));
        purchaseRepository.save(p);
        if (item.entitlementId() != null) {
            entitlementRepository.save(newEntitlement(appId, item, DEMO_END_USER_ID, p, when, now));
        }
    }

    private Purchase newPurchase(String appId, SeedItem item, String userId, PurchaseStatus status,
                                 Instant when, Random rnd) {
        Purchase p = new Purchase();
        p.setId(Ids.newId("pur"));
        p.setDeveloperAppId(appId);
        p.setUserId(userId);
        p.setItemId(item.itemId());
        p.setBillingMode(BillingMode.MOCK);
        p.setProvider(BillingProviderType.MOCK);
        p.setStatus(status);
        // Simulated transaction id. // TODO(google-billing): store the real Google order id /
        //  providerPurchaseToken here, and verify it before marking SUCCESS. Never log the full token.
        p.setProviderOrderId("mock_order_" + Long.toHexString(rnd.nextLong() & 0xffffffffL));
        // Price snapshot at purchase time (same as the live start-purchase flow).
        p.setPriceAmountMinor(item.priceMinor());
        p.setPriceCurrency("USD");
        p.setPaymentMethod(randomPaymentMethod(rnd));
        p.setCreatedAt(when);
        if (status == PurchaseStatus.SUCCESS) {
            p.setCompletedAt(when);
        }
        return p;
    }

    /** A weighted-random payment method, so the portal's revenue-by-method chart looks realistic. */
    private PaymentMethod randomPaymentMethod(Random rnd) {
        int roll = rnd.nextInt(100);
        if (roll < 40) {
            return PaymentMethod.CREDIT_CARD;   // 40%
        } else if (roll < 65) {
            return PaymentMethod.GOOGLE_PLAY;   // 25%
        } else if (roll < 85) {
            return PaymentMethod.APPLE_PAY;     // 20%
        } else {
            return PaymentMethod.PAYPAL;        // 15%
        }
    }

    private Entitlement newEntitlement(String appId, SeedItem item, String userId, Purchase purchase,
                                       Instant startsAt, Instant now) {
        Entitlement e = new Entitlement();
        e.setId(Ids.newId("ent"));
        e.setDeveloperAppId(appId);
        e.setUserId(userId);
        e.setEntitlementId(item.entitlementId());
        e.setSourceItemId(item.itemId());
        e.setPurchaseId(purchase.getId());
        e.setStartsAt(startsAt);
        if (item.subPeriodDays() > 0) {
            Instant expiresAt = startsAt.plus(item.subPeriodDays(), ChronoUnit.DAYS);
            e.setExpiresAt(expiresAt);
            // Subscriptions started long ago are already EXPIRED; recent ones are ACTIVE.
            e.setStatus(expiresAt.isAfter(now) ? EntitlementStatus.ACTIVE : EntitlementStatus.EXPIRED);
        } else {
            e.setExpiresAt(null); // lifetime / non-consumable: never expires
            e.setStatus(EntitlementStatus.ACTIVE);
        }
        return e;
    }

    private AnalyticsEvent event(String appId, String name, String itemId, String userId,
                                 Instant when, String purchaseId) {
        AnalyticsEvent e = new AnalyticsEvent();
        e.setId(Ids.newId("evt"));
        e.setDeveloperAppId(appId);
        e.setUserId(userId);
        e.setEventName(name);
        e.setBillingMode(BillingMode.MOCK);
        e.setItemId(itemId);
        e.setPurchaseId(purchaseId);
        e.setCreatedAt(when);
        return e;
    }

    // --- shaping helpers ---------------------------------------------------------------------

    /** The monthly "story" that makes revenue-over-time interesting instead of flat. */
    private double monthMultiplier(int month) {
        return switch (month) {
            case 1 -> 0.5;  // January: quiet launch month
            case 2 -> 0.7;  // February: slightly more
            case 3 -> 1.6;  // March: marketing campaign spike
            case 4 -> 1.0;  // April: stable
            case 5 -> 1.1;  // May: subscription push (see weight())
            case 6 -> 1.2;  // June: yearly-plan push (see weight())
            default -> 1.0; // later months (if the clock has advanced): stable
        };
    }

    /** Weighted catalog pick: popularity, nudged by the month's campaign focus. */
    private SeedItem pickItem(Random rnd, int month) {
        double total = 0;
        for (SeedItem it : CATALOG) {
            total += weight(it, month);
        }
        double r = rnd.nextDouble() * total;
        for (SeedItem it : CATALOG) {
            r -= weight(it, month);
            if (r <= 0) {
                return it;
            }
        }
        return CATALOG.get(CATALOG.size() - 1);
    }

    private double weight(SeedItem it, int month) {
        double w = it.popularity();
        if (month == 5 && it.type() == ItemType.SUBSCRIPTION) {
            w *= 1.6; // May: more subscriptions
        }
        if (month == 6 && it.subPeriodDays() >= 365) {
            w *= 2.0; // June: more yearly plans
        }
        return w;
    }

    /** Cheap consumables convert better; expensive subscriptions convert worse. Clamped to [0.12, 0.5]. */
    private double successRate(SeedItem it) {
        return Math.max(0.12, Math.min(0.5, 0.5 - it.priceMinor() / 20000.0));
    }

    /** A random instant during {@code day}, never in the future relative to {@code now}. */
    private Instant instantOn(LocalDate day, Random rnd, Instant now) {
        Instant ts = day.atStartOfDay(ZoneOffset.UTC).toInstant().plusSeconds(rnd.nextInt(86_400));
        return ts.isAfter(now) ? now.minusSeconds(60 + rnd.nextInt(3600)) : ts;
    }

    private SeedItem item(String itemId) {
        return CATALOG.stream().filter(it -> it.itemId().equals(itemId)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Unknown seed item: " + itemId));
    }

    // --- catalog -----------------------------------------------------------------------------

    /** One purchasable demo product. {@code subPeriodDays} is 0 for non-subscriptions. */
    private record SeedItem(String itemId, String name, String description, ItemType type,
                            String entitlementId, long priceMinor, int subPeriodDays, double popularity) {
    }

    /**
     * The demo catalog (21 items): a mix of lifetime unlocks, monthly/yearly subscriptions, consumable
     * coin packs, and feature unlocks. The first four ids are "canonical" — the SDK and the demo app
     * reference them by id. DEMO VALUES ONLY (not real Google Play Console products).
     */
    private static final List<SeedItem> CATALOG = List.of(
            new SeedItem("remove_ads", "Remove Ads", "Enjoy the app without ads, forever.",
                    ItemType.NON_CONSUMABLE, "ent_remove_ads", 499L, 0, 1.0),
            new SeedItem("premium_lifetime", "Premium (Lifetime)", "Unlock every premium feature, one-time.",
                    ItemType.NON_CONSUMABLE, "ent_premium", 999L, 0, 0.7),
            new SeedItem("coins_100", "100 Coins", "A small pack of 100 in-app coins.",
                    ItemType.CONSUMABLE, null, 99L, 0, 1.6),
            new SeedItem("monthly_subscription_demo", "Monthly Subscription (Demo)", "Demo plan, billed monthly.",
                    ItemType.SUBSCRIPTION, "ent_monthly_demo", 499L, 30, 0.6),
            new SeedItem("premium_theme", "Premium Theme Pack", "A set of premium app themes.",
                    ItemType.NON_CONSUMABLE, "ent_premium_theme", 299L, 0, 0.8),
            new SeedItem("pro_monthly", "Pro Monthly", "Pro features, billed monthly.",
                    ItemType.SUBSCRIPTION, "ent_pro", 799L, 30, 0.7),
            new SeedItem("pro_yearly", "Pro Yearly", "Pro features, billed yearly (best value).",
                    ItemType.SUBSCRIPTION, "ent_pro", 5999L, 365, 0.4),
            new SeedItem("coins_500", "500 Coins", "A pack of 500 in-app coins.",
                    ItemType.CONSUMABLE, null, 399L, 0, 1.1),
            new SeedItem("coins_1200", "1200 Coins", "A big pack of 1200 in-app coins.",
                    ItemType.CONSUMABLE, null, 799L, 0, 0.7),
            new SeedItem("extra_lives", "Extra Lives Pack", "A pack of extra lives.",
                    ItemType.CONSUMABLE, null, 199L, 0, 1.2),
            new SeedItem("advanced_analytics", "Advanced Analytics", "Unlock advanced in-app analytics.",
                    ItemType.NON_CONSUMABLE, "ent_advanced_analytics", 1499L, 0, 0.4),
            new SeedItem("starter_bundle", "Starter Bundle", "A starter bundle of perks for new players.",
                    ItemType.NON_CONSUMABLE, "ent_starter_bundle", 599L, 0, 0.9),
            new SeedItem("creator_pack", "Creator Pack", "Tools and content for creators.",
                    ItemType.NON_CONSUMABLE, "ent_creator_pack", 999L, 0, 0.5),
            new SeedItem("cloud_backup_monthly", "Cloud Backup", "Automatic cloud backup, billed monthly.",
                    ItemType.SUBSCRIPTION, "ent_cloud_backup", 299L, 30, 0.8),
            new SeedItem("ai_assistant_monthly", "AI Assistant", "AI assistant features, billed monthly.",
                    ItemType.SUBSCRIPTION, "ent_ai_assistant", 699L, 30, 0.7),
            new SeedItem("vip_badge", "VIP Badge", "Show a VIP badge on your profile.",
                    ItemType.NON_CONSUMABLE, "ent_vip", 199L, 0, 0.6),
            new SeedItem("unlimited_yearly", "Unlimited Access", "Unlimited access, billed yearly.",
                    ItemType.SUBSCRIPTION, "ent_unlimited", 3999L, 365, 0.4),
            new SeedItem("pro_theme_dark", "Pro Dark Theme", "A premium dark theme.",
                    ItemType.NON_CONSUMABLE, "ent_pro_theme_dark", 149L, 0, 0.7),
            new SeedItem("extra_content_pack", "Extra Content Pack", "A pack of extra levels and content.",
                    ItemType.NON_CONSUMABLE, "ent_extra_content", 349L, 0, 0.6),
            new SeedItem("coins_5000", "5000 Coins", "A huge pack of 5000 in-app coins.",
                    ItemType.CONSUMABLE, null, 1999L, 0, 0.5),
            new SeedItem("season_pass_yearly", "Season Pass (Yearly)", "A full-season pass, billed yearly.",
                    ItemType.SUBSCRIPTION, "ent_season_pass", 2499L, 365, 0.4)
    );
}
