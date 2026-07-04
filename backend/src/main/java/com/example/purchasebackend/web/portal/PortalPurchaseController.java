package com.example.purchasebackend.web.portal;

import com.example.purchasebackend.common.ApiException;
import com.example.purchasebackend.common.ApiResponse;
import com.example.purchasebackend.common.ErrorCode;
import com.example.purchasebackend.common.RequestContext;
import com.example.purchasebackend.domain.enums.PurchaseStatus;
import com.example.purchasebackend.dto.portal.PurchaseDtos.PagedPurchasesDto;
import com.example.purchasebackend.dto.portal.PurchaseDtos.PurchaseDetailDto;
import com.example.purchasebackend.security.PortalContext;
import com.example.purchasebackend.service.portal.PortalAppService;
import com.example.purchasebackend.service.portal.PortalPurchaseService;
import com.example.purchasebackend.service.support.PaymentMethods;
import com.example.purchasebackend.service.support.DateRanges;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Portal purchases listing + detail (JWT). */
@RestController
@RequestMapping("/api/v1/portal/apps/{appId}/purchases")
public class PortalPurchaseController {

    private final PortalPurchaseService purchaseService;
    private final PortalAppService appService;

    public PortalPurchaseController(PortalPurchaseService purchaseService, PortalAppService appService) {
        this.purchaseService = purchaseService;
        this.appService = appService;
    }

    @GetMapping
    public ApiResponse<PagedPurchasesDto> list(
            @PathVariable String appId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String itemId,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        authorize(appId);
        // Clamp the page size so a single request can never pull the whole history.
        int safeSize = Math.min(Math.max(1, size), 200);
        PagedPurchasesDto data = purchaseService.list(
                appId, DateRanges.resolveFrom(from), DateRanges.resolveTo(to),
                parseStatus(status), itemId, PaymentMethods.parseOrNull(paymentMethod), userId,
                Math.max(0, page), safeSize);
        return ApiResponse.ok(data, RequestContext.getRequestId());
    }

    @GetMapping("/{purchaseId}")
    public ApiResponse<PurchaseDetailDto> detail(@PathVariable String appId, @PathVariable String purchaseId) {
        authorize(appId);
        return ApiResponse.ok(purchaseService.detail(appId, purchaseId), RequestContext.getRequestId());
    }

    private void authorize(String appId) {
        appService.requireOwnedApp(appId, PortalContext.requireUser());
    }

    private PurchaseStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return PurchaseStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Invalid purchase status: " + status);
        }
    }
}
