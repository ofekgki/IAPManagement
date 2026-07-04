package com.example.purchasebackend.web;

import com.example.purchasebackend.common.ApiResponse;
import com.example.purchasebackend.common.RequestContext;
import com.example.purchasebackend.domain.DeveloperApp;
import com.example.purchasebackend.dto.sdk.CheckEntitlementResponse;
import com.example.purchasebackend.dto.sdk.EntitlementDto;
import com.example.purchasebackend.dto.sdk.EntitlementsResponse;
import com.example.purchasebackend.service.EntitlementService;
import com.example.purchasebackend.service.mapper.DtoMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** SDK entitlement endpoints backing the SDK's hasEntitlement / listEntitlements. */
@RestController
@RequestMapping("/api/v1/sdk/entitlements")
public class EntitlementController {

    private final EntitlementService entitlementService;

    public EntitlementController(EntitlementService entitlementService) {
        this.entitlementService = entitlementService;
    }

    /**
     * GET /check — resolve a single entitlement by {@code entitlementId} or {@code itemId}. Backs the
     * SDK's {@code hasEntitlement(...)}.
     */
    @GetMapping("/check")
    public ApiResponse<CheckEntitlementResponse> check(
            @RequestParam String userId,
            @RequestParam(required = false) String entitlementId,
            @RequestParam(required = false) String itemId) {
        DeveloperApp app = RequestContext.requireDeveloperApp();
        CheckEntitlementResponse data = entitlementService.checkEntitlement(app, userId, entitlementId, itemId);
        return ApiResponse.ok(data, RequestContext.getRequestId());
    }

    /** GET /entitlements — all of a user's entitlements (active + expired/revoked). */
    @GetMapping
    public ApiResponse<EntitlementsResponse> list(@RequestParam String userId) {
        DeveloperApp app = RequestContext.requireDeveloperApp();
        List<EntitlementDto> entitlements = entitlementService.listEntitlements(app.getId(), userId).stream()
                .map(DtoMapper::toEntitlementDto)
                .toList();
        return ApiResponse.ok(new EntitlementsResponse(entitlements), RequestContext.getRequestId());
    }
}
