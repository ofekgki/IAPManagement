package com.example.purchasebackend.service;

import com.example.purchasebackend.common.ApiException;
import com.example.purchasebackend.common.ErrorCode;
import com.example.purchasebackend.common.Ids;
import com.example.purchasebackend.domain.DeveloperApp;
import com.example.purchasebackend.domain.Entitlement;
import com.example.purchasebackend.domain.Purchase;
import com.example.purchasebackend.domain.PurchaseItem;
import com.example.purchasebackend.domain.enums.BillingMode;
import com.example.purchasebackend.domain.enums.BillingProviderType;
import com.example.purchasebackend.domain.enums.EntitlementStatus;
import com.example.purchasebackend.domain.enums.PurchaseStatus;
import com.example.purchasebackend.dto.internal.RevokeEntitlementRequest;
import com.example.purchasebackend.dto.sdk.ConfirmPurchaseRequest;
import com.example.purchasebackend.dto.sdk.ConfirmPurchaseResponse;
import com.example.purchasebackend.dto.sdk.EntitlementSummaryDto;
import com.example.purchasebackend.dto.sdk.GooglePlayConfirmData;
import com.example.purchasebackend.dto.sdk.RestoreRequest;
import com.example.purchasebackend.dto.sdk.RestoreResponse;
import com.example.purchasebackend.dto.sdk.RestoredPurchaseDto;
import com.example.purchasebackend.dto.sdk.StartPurchaseRequest;
import com.example.purchasebackend.dto.sdk.StartPurchaseResponse;
import com.example.purchasebackend.repository.EntitlementRepository;
import com.example.purchasebackend.repository.PurchaseRepository;
import com.example.purchasebackend.service.googleplay.GooglePlayVerificationRequest;
import com.example.purchasebackend.service.googleplay.GooglePlayVerificationService;
import com.example.purchasebackend.service.googleplay.VerificationResult;
import com.example.purchasebackend.service.mapper.DtoMapper;
import com.example.purchasebackend.service.support.BillingModes;
import com.example.purchasebackend.service.support.PaymentMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The purchase flow: start, confirm (MOCK fully; GOOGLE_PLAY scaffolded + fails safely), and restore.
 *
 * <p>Confirmation is idempotent via the Idempotency-Key header. Entitlements are only ever granted
 * after a successful MOCK confirmation or a <em>verified</em> Google purchase — never from an
 * unverified Google token.
 */
@Service
public class PurchaseService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseService.class);

    private final PurchaseRepository purchaseRepository;
    private final EntitlementRepository entitlementRepository;
    private final ItemService itemService;
    private final EntitlementService entitlementService;
    private final AnalyticsService analyticsService;
    private final IdempotencyService idempotencyService;
    private final MockBillingService mockBillingService;
    private final GooglePlayVerificationService googlePlayVerificationService;

    public PurchaseService(PurchaseRepository purchaseRepository,
                           EntitlementRepository entitlementRepository,
                           ItemService itemService,
                           EntitlementService entitlementService,
                           AnalyticsService analyticsService,
                           IdempotencyService idempotencyService,
                           MockBillingService mockBillingService,
                           GooglePlayVerificationService googlePlayVerificationService) {
        this.purchaseRepository = purchaseRepository;
        this.entitlementRepository = entitlementRepository;
        this.itemService = itemService;
        this.entitlementService = entitlementService;
        this.analyticsService = analyticsService;
        this.idempotencyService = idempotencyService;
        this.mockBillingService = mockBillingService;
        this.googlePlayVerificationService = googlePlayVerificationService;
    }

    // --- start ------------------------------------------------------------------------------

    public StartPurchaseResponse startPurchase(DeveloperApp app, StartPurchaseRequest req) {
        if (req.userId() == null || req.userId().isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "userId is required.");
        }
        BillingMode billingMode = BillingModes.parseOrDefault(req.billingMode(), app.getBillingModeDefault());
        PurchaseItem item = itemService.getActiveItem(app.getId(), req.itemId());

        Purchase purchase = new Purchase();
        purchase.setId(Ids.newId("pur"));
        purchase.setDeveloperAppId(app.getId());
        purchase.setUserId(req.userId());
        purchase.setItemId(item.getItemId());
        purchase.setBillingMode(billingMode);
        purchase.setProvider(providerFor(billingMode));
        purchase.setStatus(PurchaseStatus.CREATED);
        // Price snapshot: revenue is computed from the price the user was shown, so a later edit of
        // the item's list price never rewrites historical revenue.
        purchase.setPriceAmountMinor(item.getPriceAmountMinor());
        purchase.setPriceCurrency(item.getCurrency());
        // How the user chose to pay (recorded for the portal's revenue-by-payment-method breakdown).
        purchase.setPaymentMethod(PaymentMethods.parseOrDefault(req.paymentMethod(), PaymentMethods.DEFAULT));
        purchaseRepository.save(purchase);

        analyticsService.record(app.getId(), req.userId(), "purchase_started", billingMode,
                item.getItemId(), purchase.getId(), null);
        log.info("purchase started app={} user={} item={} mode={} purchaseId={}",
                app.getId(), req.userId(), item.getItemId(), billingMode, purchase.getId());

        return new StartPurchaseResponse(
                purchase.getId(), purchase.getStatus().name(), billingMode.name(), item.getItemId());
    }

    // --- confirm ----------------------------------------------------------------------------

    public ConfirmPurchaseResponse confirmPurchase(DeveloperApp app, ConfirmPurchaseRequest req,
                                                   String idempotencyKey) {
        // 1. Replay a previously processed idempotent request.
        Optional<ConfirmPurchaseResponse> replay =
                idempotencyService.findStored(app.getId(), idempotencyKey, ConfirmPurchaseResponse.class);
        if (replay.isPresent()) {
            log.info("idempotent replay key={} app={}", idempotencyKey, app.getId());
            return replay.get();
        }

        BillingMode billingMode = BillingModes.parseOrDefault(req.billingMode(), app.getBillingModeDefault());

        // 2. Load + validate the purchase.
        Purchase purchase = purchaseRepository.findByIdAndDeveloperAppId(req.purchaseId(), app.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.PURCHASE_NOT_FOUND));
        if (!purchase.getUserId().equals(req.userId()) || !purchase.getItemId().equals(req.itemId())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST,
                    "purchase does not match the provided userId/itemId.");
        }
        if (purchase.getStatus() == PurchaseStatus.CANCELLED) {
            throw new ApiException(ErrorCode.PURCHASE_CANCELLED);
        }

        PurchaseItem item = itemService.getActiveItem(app.getId(), purchase.getItemId());

        // 3. Already successful → return the original result (idempotent without a key too).
        if (purchase.getStatus() == PurchaseStatus.SUCCESS) {
            ConfirmPurchaseResponse response = buildResponse(purchase, findEntitlement(purchase, item));
            idempotencyService.store(app.getId(), idempotencyKey, purchase.getId(), response);
            return response;
        }

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            purchase.setIdempotencyKey(idempotencyKey);
        }

        // 4. Fulfill per billing mode.
        return switch (billingMode) {
            case MOCK -> confirmMock(app, purchase, item, idempotencyKey);
            case GOOGLE_PLAY -> confirmGooglePlay(app, purchase, item, req.googlePlay(), idempotencyKey);
        };
    }

    private ConfirmPurchaseResponse confirmMock(DeveloperApp app, Purchase purchase, PurchaseItem item,
                                                String idempotencyKey) {
        mockBillingService.confirm(purchase);
        purchase.setProvider(BillingProviderType.MOCK);
        purchaseRepository.save(purchase);

        Entitlement entitlement = entitlementService.grantForPurchase(purchase, item, null);

        analyticsService.record(app.getId(), purchase.getUserId(), "purchase_success",
                BillingMode.MOCK, item.getItemId(), purchase.getId(), null);
        log.info("purchase confirmed (MOCK) purchaseId={} entitlement={}",
                purchase.getId(), entitlement != null ? entitlement.getEntitlementId() : "none");

        ConfirmPurchaseResponse response = buildResponse(purchase, entitlement);
        idempotencyService.store(app.getId(), idempotencyKey, purchase.getId(), response);
        return response;
    }

    private ConfirmPurchaseResponse confirmGooglePlay(DeveloperApp app, Purchase purchase, PurchaseItem item,
                                                      GooglePlayConfirmData googlePlay, String idempotencyKey) {
        purchase.setProvider(BillingProviderType.GOOGLE_PLAY);
        if (googlePlay != null) {
            // Stored for verification; never logged in full (see GooglePlayVerificationService).
            purchase.setProviderPurchaseToken(googlePlay.purchaseToken());
            purchase.setProviderOrderId(googlePlay.orderId());
        }

        GooglePlayVerificationRequest verificationRequest = new GooglePlayVerificationRequest(
                app.getId(),
                app.getPackageName(),
                googlePlay != null ? googlePlay.productId() : item.getGooglePlayProductId(),
                googlePlay != null ? googlePlay.purchaseToken() : null,
                googlePlay != null ? googlePlay.orderId() : null,
                purchase.getUserId(),
                item.getType());
        VerificationResult result = googlePlayVerificationService.verifyPurchase(verificationRequest);

        return switch (result.outcome()) {
            case NOT_CONFIGURED -> {
                // Fail safely: record why, never grant.
                purchase.setStatus(PurchaseStatus.REQUIRES_VERIFICATION);
                purchase.setFailureCode(ErrorCode.GOOGLE_PLAY_NOT_CONFIGURED.name());
                purchase.setFailureMessage(result.message());
                purchaseRepository.save(purchase);
                analyticsService.record(app.getId(), purchase.getUserId(), "purchase_failed",
                        BillingMode.GOOGLE_PLAY, item.getItemId(), purchase.getId(), null);
                throw new ApiException(ErrorCode.GOOGLE_PLAY_NOT_CONFIGURED, result.message());
            }
            case FAILED -> {
                purchase.setStatus(PurchaseStatus.FAILED);
                purchase.setFailureCode(ErrorCode.PURCHASE_VERIFICATION_FAILED.name());
                purchase.setFailureMessage(result.message());
                purchaseRepository.save(purchase);
                analyticsService.record(app.getId(), purchase.getUserId(), "purchase_failed",
                        BillingMode.GOOGLE_PLAY, item.getItemId(), purchase.getId(), null);
                throw new ApiException(ErrorCode.PURCHASE_VERIFICATION_FAILED, result.message());
            }
            case VERIFIED -> {
                // TODO: Acknowledge non-consumable/subscription purchases; consume consumables.
                purchase.setStatus(PurchaseStatus.SUCCESS);
                purchase.setCompletedAt(Instant.now());
                purchaseRepository.save(purchase);
                Entitlement entitlement =
                        entitlementService.grantForPurchase(purchase, item, result.subscriptionExpiry());
                analyticsService.record(app.getId(), purchase.getUserId(), "purchase_success",
                        BillingMode.GOOGLE_PLAY, item.getItemId(), purchase.getId(), null);
                ConfirmPurchaseResponse response = buildResponse(purchase, entitlement);
                idempotencyService.store(app.getId(), idempotencyKey, purchase.getId(), response);
                yield response;
            }
        };
    }

    // --- restore ----------------------------------------------------------------------------

    public RestoreResponse restorePurchases(DeveloperApp app, RestoreRequest req) {
        if (req.userId() == null || req.userId().isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "userId is required.");
        }
        BillingMode billingMode = BillingModes.parseOrDefault(req.billingMode(), app.getBillingModeDefault());
        analyticsService.record(app.getId(), req.userId(), "restore_started", billingMode, null, null, null);

        if (billingMode == BillingMode.GOOGLE_PLAY) {
            // TODO: Client SDK should query Google Play Billing for owned purchases.
            // TODO: Client SDK should send purchase tokens to this backend.
            // TODO: Backend should verify each token before restoring entitlement.
            analyticsService.record(app.getId(), req.userId(), "restore_failed", billingMode, null, null, null);
            throw new ApiException(ErrorCode.GOOGLE_PLAY_NOT_CONFIGURED);
        }

        // The user's successful purchases, deduped by item. When the request names a single item
        // (per-item return), narrow to just that item; otherwise every owned item is returnable.
        String onlyItemId = (req.itemId() != null && !req.itemId().isBlank()) ? req.itemId() : null;
        Map<String, Purchase> ownedByItem = new LinkedHashMap<>();
        for (Purchase p : purchaseRepository
                .findByDeveloperAppIdAndUserIdAndStatus(app.getId(), req.userId(), PurchaseStatus.SUCCESS)) {
            if (onlyItemId != null && !onlyItemId.equals(p.getItemId())) {
                continue;
            }
            ownedByItem.putIfAbsent(p.getItemId(), p);
        }

        // Current access per item. Restore = RETURN/refund: only items the user CURRENTLY owns (ACTIVE
        // entitlement) can be returned. Returning writes a RESTORED purchase receipt (the portal
        // subtracts its item price from revenue) and revokes the entitlement, so the user no longer
        // owns it and the store shows "Buy" again. Idempotent: after a return the item is REVOKED, so a
        // second restore skips it; re-buying makes it ACTIVE again and returnable again.
        List<Entitlement> userEntitlements = entitlementService.listEntitlements(app.getId(), req.userId());
        Map<String, EntitlementStatus> accessByItem = new HashMap<>();
        for (Entitlement e : userEntitlements) {
            if (e.getSourceItemId() == null) {
                continue;
            }
            EntitlementStatus current = accessByItem.get(e.getSourceItemId());
            if (current == null || e.getStatus() == EntitlementStatus.ACTIVE) {
                accessByItem.put(e.getSourceItemId(), e.getStatus());
            }
        }

        List<RestoredPurchaseDto> restored = new ArrayList<>();
        for (Purchase p : ownedByItem.values()) {
            // Only currently-owned items are returnable.
            if (accessByItem.get(p.getItemId()) != EntitlementStatus.ACTIVE) {
                continue;
            }
            PurchaseItem item = itemService.findItem(app.getId(), p.getItemId()).orElse(null);
            String itemName = item != null ? item.getName() : p.getItemId();

            Purchase receipt = new Purchase();
            receipt.setId(Ids.newId("pur"));
            receipt.setDeveloperAppId(app.getId());
            receipt.setUserId(req.userId());
            receipt.setItemId(p.getItemId());
            receipt.setBillingMode(billingMode);
            receipt.setProvider(providerFor(billingMode));
            receipt.setStatus(PurchaseStatus.RESTORED);
            receipt.setCompletedAt(Instant.now());
            // Refund exactly what was paid: copy the original purchase's price snapshot (falling
            // back to the item's current price for legacy purchases without one).
            receipt.setPriceAmountMinor(p.getPriceAmountMinor() != null ? p.getPriceAmountMinor()
                    : (item != null ? item.getPriceAmountMinor() : null));
            receipt.setPriceCurrency(p.getPriceCurrency() != null ? p.getPriceCurrency()
                    : (item != null ? item.getCurrency() : null));
            // A refund is attributed to the same payment method as the original purchase.
            receipt.setPaymentMethod(p.getPaymentMethod());
            purchaseRepository.save(receipt);

            // Return it: revoke the entitlement so ownership is released.
            if (item != null && item.getEntitlementId() != null && !item.getEntitlementId().isBlank()) {
                entitlementService.revoke(new RevokeEntitlementRequest(
                        app.getId(), req.userId(), item.getEntitlementId(), "restore_return"));
            }

            restored.add(new RestoredPurchaseDto(
                    receipt.getId(), p.getItemId(), itemName,
                    PurchaseStatus.RESTORED.name(), "RETURNED"));
        }

        List<EntitlementSummaryDto> entitlements = userEntitlements.stream()
                .filter(e -> e.getStatus() == EntitlementStatus.ACTIVE)
                .map(DtoMapper::toEntitlementSummary)
                .toList();

        analyticsService.record(app.getId(), req.userId(), "restore_success", billingMode, null, null, null);
        log.info("restore app={} user={} restored={} active_entitlements={}",
                app.getId(), req.userId(), restored.size(), entitlements.size());
        return new RestoreResponse(restored, entitlements);
    }

    // --- internal listing -------------------------------------------------------------------

    public List<Purchase> listPurchases(String developerAppId, String userId, PurchaseStatus status) {
        if (userId != null && !userId.isBlank() && status != null) {
            return purchaseRepository.findByDeveloperAppIdAndUserId(developerAppId, userId)
                    .stream().filter(p -> p.getStatus() == status).toList();
        }
        if (userId != null && !userId.isBlank()) {
            return purchaseRepository.findByDeveloperAppIdAndUserId(developerAppId, userId);
        }
        if (status != null) {
            return purchaseRepository.findByDeveloperAppIdAndStatus(developerAppId, status);
        }
        return purchaseRepository.findByDeveloperAppId(developerAppId);
    }

    // --- helpers ----------------------------------------------------------------------------

    private ConfirmPurchaseResponse buildResponse(Purchase purchase, Entitlement entitlement) {
        EntitlementSummaryDto summary = entitlement == null ? null
                : new EntitlementSummaryDto(
                        entitlement.getEntitlementId(), entitlement.getStatus().name(), entitlement.getExpiresAt());
        return new ConfirmPurchaseResponse(
                purchase.getId(), purchase.getStatus().name(), entitlement != null, summary);
    }

    private Entitlement findEntitlement(Purchase purchase, PurchaseItem item) {
        String entitlementId = item.getEntitlementId();
        if (entitlementId == null || entitlementId.isBlank()) {
            return null;
        }
        return entitlementRepository
                .findFirstByDeveloperAppIdAndUserIdAndEntitlementIdOrderByCreatedAtDesc(
                        purchase.getDeveloperAppId(), purchase.getUserId(), entitlementId)
                .orElse(null);
    }

    private BillingProviderType providerFor(BillingMode billingMode) {
        return billingMode == BillingMode.GOOGLE_PLAY
                ? BillingProviderType.GOOGLE_PLAY
                : BillingProviderType.MOCK;
    }
}
