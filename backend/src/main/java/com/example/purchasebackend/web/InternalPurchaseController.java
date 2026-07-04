package com.example.purchasebackend.web;

import com.example.purchasebackend.common.ApiException;
import com.example.purchasebackend.common.ApiResponse;
import com.example.purchasebackend.common.ErrorCode;
import com.example.purchasebackend.common.RequestContext;
import com.example.purchasebackend.domain.enums.PurchaseStatus;
import com.example.purchasebackend.dto.internal.AdminPurchaseDto;
import com.example.purchasebackend.service.PurchaseService;
import com.example.purchasebackend.service.mapper.DtoMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Internal/admin purchase listing with filters. Guarded by {@code InternalAdminTokenFilter}. */
@RestController
@RequestMapping("/api/v1/internal/purchases")
public class InternalPurchaseController {

    private final PurchaseService purchaseService;

    public InternalPurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @GetMapping
    public ApiResponse<List<AdminPurchaseDto>> list(
            @RequestParam String developerAppId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String status) {
        PurchaseStatus statusFilter = parseStatus(status);
        List<AdminPurchaseDto> data = purchaseService.listPurchases(developerAppId, userId, statusFilter).stream()
                .map(DtoMapper::toAdminPurchaseDto)
                .toList();
        return ApiResponse.ok(data, RequestContext.getRequestId());
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
