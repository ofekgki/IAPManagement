package com.example.purchasebackend.service;

import com.example.purchasebackend.common.ApiException;
import com.example.purchasebackend.common.ErrorCode;
import com.example.purchasebackend.common.Ids;
import com.example.purchasebackend.domain.DeveloperApp;
import com.example.purchasebackend.domain.Entitlement;
import com.example.purchasebackend.domain.Purchase;
import com.example.purchasebackend.domain.PurchaseItem;
import com.example.purchasebackend.domain.enums.EntitlementStatus;
import com.example.purchasebackend.dto.internal.GrantEntitlementRequest;
import com.example.purchasebackend.dto.internal.RevokeEntitlementRequest;
import com.example.purchasebackend.dto.sdk.CheckEntitlementResponse;
import com.example.purchasebackend.repository.EntitlementRepository;
import com.example.purchasebackend.repository.PurchaseItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Owns entitlement granting, checking, listing, and admin grant/revoke — plus lazy expiry handling.
 *
 * <p>Rules:
 * <ul>
 *   <li>NON_CONSUMABLE → active forever; never duplicated (returns the existing active one).</li>
 *   <li>SUBSCRIPTION → active with an expiry (MOCK: configurable demo days; GOOGLE_PLAY: from verified
 *       Google data — // TODO: Get real subscription expiry from Google Play Developer API).</li>
 *   <li>CONSUMABLE → no long-lived entitlement unless the item declares an {@code entitlementId}.
 *       // TODO: Add consumable balance support, for example coins or credits.</li>
 * </ul>
 */
@Service
public class EntitlementService {

    private static final Logger log = LoggerFactory.getLogger(EntitlementService.class);

    private final EntitlementRepository entitlementRepository;
    private final PurchaseItemRepository itemRepository;
    private final AnalyticsService analyticsService;
    private final int mockSubscriptionDays;

    public EntitlementService(EntitlementRepository entitlementRepository,
                              PurchaseItemRepository itemRepository,
                              AnalyticsService analyticsService,
                              @Value("${app.mock-subscription-days:30}") int mockSubscriptionDays) {
        this.entitlementRepository = entitlementRepository;
        this.itemRepository = itemRepository;
        this.analyticsService = analyticsService;
        this.mockSubscriptionDays = mockSubscriptionDays;
    }

    /**
     * Grants (or returns the existing) entitlement for a successful purchase. Returns null when the
     * item is a consumable with no entitlementId (no long-lived access).
     *
     * @param subscriptionExpiryOverride expiry from verified Google data; null → MOCK demo expiry.
     */
    public Entitlement grantForPurchase(Purchase purchase, PurchaseItem item, Instant subscriptionExpiryOverride) {
        String entitlementId = item.getEntitlementId();
        if (entitlementId == null || entitlementId.isBlank()) {
            log.debug("no entitlement granted (no entitlementId) item={}", item.getItemId());
            return null;
        }

        switch (item.getType()) {
            case NON_CONSUMABLE -> {
                Entitlement existing = latest(purchase.getDeveloperAppId(), purchase.getUserId(), entitlementId);
                if (existing != null && existing.getStatus() == EntitlementStatus.ACTIVE) {
                    return existing; // already owned forever; do not duplicate
                }
                return create(purchase, item, entitlementId, null);
            }
            case SUBSCRIPTION -> {
                Instant expiry = subscriptionExpiryOverride != null
                        ? subscriptionExpiryOverride
                        : Instant.now().plus(mockSubscriptionDays, ChronoUnit.DAYS);
                return create(purchase, item, entitlementId, expiry);
            }
            case CONSUMABLE -> {
                // Item explicitly declares an entitlementId → treat as a simple owned flag.
                // TODO: replace with a consumable balance (e.g. add coins) instead of a flag.
                return create(purchase, item, entitlementId, null);
            }
            default -> {
                return null;
            }
        }
    }

    /** Resolves and checks a single entitlement for a user. */
    public CheckEntitlementResponse checkEntitlement(DeveloperApp app, String userId,
                                                     String entitlementId, String itemId) {
        if ((userId == null || userId.isBlank())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "userId is required.");
        }
        String resolvedEntitlementId = resolveEntitlementId(app.getId(), entitlementId, itemId);
        if (resolvedEntitlementId == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST,
                    "Provide entitlementId or a known itemId.");
        }

        Entitlement entitlement = latest(app.getId(), userId, resolvedEntitlementId);
        boolean has = false;
        String status = null;
        Instant expiresAt = null;
        if (entitlement != null) {
            refreshExpiry(entitlement);
            status = entitlement.getStatus().name();
            expiresAt = entitlement.getExpiresAt();
            has = entitlement.getStatus() == EntitlementStatus.ACTIVE;
        }

        analyticsService.record(app.getId(), userId, "entitlement_checked", null,
                itemId, null, null);
        return new CheckEntitlementResponse(has, resolvedEntitlementId, status, expiresAt);
    }

    /** Returns all of a user's entitlements (with expiry refreshed). */
    public List<Entitlement> listEntitlements(String developerAppId, String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "userId is required.");
        }
        List<Entitlement> all = entitlementRepository.findByDeveloperAppIdAndUserId(developerAppId, userId);
        all.forEach(this::refreshExpiry);
        return all;
    }

    /** Admin: grant an entitlement manually. */
    public Entitlement grantManual(GrantEntitlementRequest req) {
        Entitlement entitlement = new Entitlement();
        entitlement.setId(Ids.newId("ent"));
        entitlement.setDeveloperAppId(req.developerAppId());
        entitlement.setUserId(req.userId());
        entitlement.setEntitlementId(req.entitlementId());
        entitlement.setSourceItemId(req.sourceItemId());
        entitlement.setStatus(EntitlementStatus.ACTIVE);
        entitlement.setStartsAt(Instant.now());
        entitlement.setExpiresAt(req.expiresAt());
        // TODO: Add audit table for manual entitlement changes.
        log.info("manual entitlement grant app={} user={} entitlement={} reason={}",
                req.developerAppId(), req.userId(), req.entitlementId(), req.reason());
        return entitlementRepository.save(entitlement);
    }

    /** Admin: revoke an entitlement (kept as a record, status set to REVOKED). */
    public Entitlement revoke(RevokeEntitlementRequest req) {
        Entitlement entitlement = latest(req.developerAppId(), req.userId(), req.entitlementId());
        if (entitlement == null) {
            throw new ApiException(ErrorCode.ENTITLEMENT_NOT_FOUND);
        }
        entitlement.setStatus(EntitlementStatus.REVOKED);
        log.info("entitlement revoked app={} user={} entitlement={} reason={}",
                req.developerAppId(), req.userId(), req.entitlementId(), req.reason());
        return entitlementRepository.save(entitlement);
    }

    // --- helpers -----------------------------------------------------------------------------

    private Entitlement create(Purchase purchase, PurchaseItem item, String entitlementId, Instant expiresAt) {
        Entitlement entitlement = new Entitlement();
        entitlement.setId(Ids.newId("ent"));
        entitlement.setDeveloperAppId(purchase.getDeveloperAppId());
        entitlement.setUserId(purchase.getUserId());
        entitlement.setEntitlementId(entitlementId);
        entitlement.setSourceItemId(item.getItemId());
        entitlement.setPurchaseId(purchase.getId());
        entitlement.setStatus(EntitlementStatus.ACTIVE);
        entitlement.setStartsAt(Instant.now());
        entitlement.setExpiresAt(expiresAt);
        log.info("entitlement granted app={} user={} entitlement={} expiresAt={}",
                purchase.getDeveloperAppId(), purchase.getUserId(), entitlementId, expiresAt);
        return entitlementRepository.save(entitlement);
    }

    private Entitlement latest(String developerAppId, String userId, String entitlementId) {
        return entitlementRepository
                .findFirstByDeveloperAppIdAndUserIdAndEntitlementIdOrderByCreatedAtDesc(
                        developerAppId, userId, entitlementId)
                .orElse(null);
    }

    private String resolveEntitlementId(String developerAppId, String entitlementId, String itemId) {
        if (entitlementId != null && !entitlementId.isBlank()) {
            return entitlementId;
        }
        if (itemId != null && !itemId.isBlank()) {
            return itemRepository.findByDeveloperAppIdAndItemId(developerAppId, itemId)
                    .map(PurchaseItem::getEntitlementId)
                    .filter(id -> id != null && !id.isBlank())
                    .orElse(null);
        }
        return null;
    }

    /** Lazily flips an ACTIVE-but-past-expiry entitlement to EXPIRED and persists it. */
    private void refreshExpiry(Entitlement entitlement) {
        if (entitlement.getStatus() == EntitlementStatus.ACTIVE
                && entitlement.getExpiresAt() != null
                && Instant.now().isAfter(entitlement.getExpiresAt())) {
            entitlement.setStatus(EntitlementStatus.EXPIRED);
            entitlementRepository.save(entitlement);
        }
    }
}
