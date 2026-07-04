package com.example.purchasebackend.web.portal;

import com.example.purchasebackend.common.ApiException;
import com.example.purchasebackend.common.ApiResponse;
import com.example.purchasebackend.common.ErrorCode;
import com.example.purchasebackend.common.RequestContext;
import com.example.purchasebackend.domain.enums.EntitlementStatus;
import com.example.purchasebackend.dto.portal.EntitlementDtos.GrantRequest;
import com.example.purchasebackend.dto.portal.EntitlementDtos.PortalEntitlementDto;
import com.example.purchasebackend.dto.portal.EntitlementDtos.RevokeRequest;
import com.example.purchasebackend.security.PortalContext;
import com.example.purchasebackend.service.portal.PortalAppService;
import com.example.purchasebackend.service.portal.PortalEntitlementService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Portal entitlements: list + manual grant/revoke (OWNER/ADMIN only; enforced in the service). */
@RestController
@RequestMapping("/api/v1/portal/apps/{appId}/entitlements")
public class PortalEntitlementController {

    private final PortalEntitlementService entitlementService;
    private final PortalAppService appService;

    public PortalEntitlementController(PortalEntitlementService entitlementService, PortalAppService appService) {
        this.entitlementService = entitlementService;
        this.appService = appService;
    }

    @GetMapping
    public ApiResponse<List<PortalEntitlementDto>> list(
            @PathVariable String appId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String entitlementId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String itemId) {
        appService.requireOwnedApp(appId, PortalContext.requireUser());
        List<PortalEntitlementDto> data = entitlementService.list(appId, userId, entitlementId,
                parseStatus(status), itemId);
        return ApiResponse.ok(data, RequestContext.getRequestId());
    }

    @PostMapping("/grant")
    public ApiResponse<PortalEntitlementDto> grant(@PathVariable String appId,
                                                   @Valid @RequestBody GrantRequest request) {
        appService.requireOwnedApp(appId, PortalContext.requireUser());
        return ApiResponse.ok(entitlementService.grant(appId, PortalContext.requireUser(), request),
                RequestContext.getRequestId());
    }

    @PostMapping("/revoke")
    public ApiResponse<PortalEntitlementDto> revoke(@PathVariable String appId,
                                                    @Valid @RequestBody RevokeRequest request) {
        appService.requireOwnedApp(appId, PortalContext.requireUser());
        return ApiResponse.ok(entitlementService.revoke(appId, PortalContext.requireUser(), request),
                RequestContext.getRequestId());
    }

    private EntitlementStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return EntitlementStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Invalid entitlement status: " + status);
        }
    }
}
