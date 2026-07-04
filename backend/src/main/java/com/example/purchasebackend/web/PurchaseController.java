package com.example.purchasebackend.web;

import com.example.purchasebackend.common.ApiResponse;
import com.example.purchasebackend.common.RequestContext;
import com.example.purchasebackend.domain.DeveloperApp;
import com.example.purchasebackend.dto.sdk.ConfirmPurchaseRequest;
import com.example.purchasebackend.dto.sdk.ConfirmPurchaseResponse;
import com.example.purchasebackend.dto.sdk.RestoreRequest;
import com.example.purchasebackend.dto.sdk.RestoreResponse;
import com.example.purchasebackend.dto.sdk.StartPurchaseRequest;
import com.example.purchasebackend.dto.sdk.StartPurchaseResponse;
import com.example.purchasebackend.service.PurchaseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** SDK purchase endpoints: start, confirm (idempotent), and restore. */
@RestController
@RequestMapping("/api/v1/sdk/purchases")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    /**
     * POST /start — creates a CREATED purchase.
     *
     * <p>// TODO: For GOOGLE_PLAY mode, the client SDK must launch BillingClient.launchBillingFlow(...).
     * <p>// TODO: The final payment UI is controlled by Google Play and cannot be replaced by this
     * backend or SDK.
     */
    @PostMapping("/start")
    public ApiResponse<StartPurchaseResponse> start(@Valid @RequestBody StartPurchaseRequest request) {
        DeveloperApp app = RequestContext.requireDeveloperApp();
        StartPurchaseResponse data = purchaseService.startPurchase(app, request);
        return ApiResponse.ok(data, RequestContext.getRequestId());
    }

    /**
     * POST /confirm — completes a purchase. Idempotent via the optional {@code Idempotency-Key}
     * header: repeating the same key returns the original result instead of processing twice.
     */
    @PostMapping("/confirm")
    public ApiResponse<ConfirmPurchaseResponse> confirm(
            @Valid @RequestBody ConfirmPurchaseRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        DeveloperApp app = RequestContext.requireDeveloperApp();
        ConfirmPurchaseResponse data = purchaseService.confirmPurchase(app, request, idempotencyKey);
        return ApiResponse.ok(data, RequestContext.getRequestId());
    }

    /** POST /restore — returns prior successful purchases and active entitlements (MOCK). */
    @PostMapping("/restore")
    public ApiResponse<RestoreResponse> restore(@Valid @RequestBody RestoreRequest request) {
        DeveloperApp app = RequestContext.requireDeveloperApp();
        RestoreResponse data = purchaseService.restorePurchases(app, request);
        return ApiResponse.ok(data, RequestContext.getRequestId());
    }
}
