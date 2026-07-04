package com.example.purchasebackend.web;

import com.example.purchasebackend.common.ApiResponse;
import com.example.purchasebackend.common.RequestContext;
import com.example.purchasebackend.domain.Entitlement;
import com.example.purchasebackend.dto.internal.EntitlementActionResponse;
import com.example.purchasebackend.dto.internal.GrantEntitlementRequest;
import com.example.purchasebackend.dto.internal.RevokeEntitlementRequest;
import com.example.purchasebackend.service.EntitlementService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Internal/admin manual entitlement grant/revoke. Guarded by {@code InternalAdminTokenFilter}. */
@RestController
@RequestMapping("/api/v1/internal/entitlements")
public class InternalEntitlementController {

    private final EntitlementService entitlementService;

    public InternalEntitlementController(EntitlementService entitlementService) {
        this.entitlementService = entitlementService;
    }

    /** POST /internal/entitlements/grant — manually grant an entitlement (reason is logged). */
    @PostMapping("/grant")
    public ApiResponse<EntitlementActionResponse> grant(@Valid @RequestBody GrantEntitlementRequest request) {
        Entitlement entitlement = entitlementService.grantManual(request);
        return ApiResponse.ok(toResponse(entitlement), RequestContext.getRequestId());
    }

    /** POST /internal/entitlements/revoke — mark an entitlement REVOKED (record is kept). */
    @PostMapping("/revoke")
    public ApiResponse<EntitlementActionResponse> revoke(@Valid @RequestBody RevokeEntitlementRequest request) {
        Entitlement entitlement = entitlementService.revoke(request);
        return ApiResponse.ok(toResponse(entitlement), RequestContext.getRequestId());
    }

    private EntitlementActionResponse toResponse(Entitlement e) {
        return new EntitlementActionResponse(
                e.getId(), e.getEntitlementId(), e.getUserId(), e.getStatus().name(), e.getExpiresAt());
    }
}
