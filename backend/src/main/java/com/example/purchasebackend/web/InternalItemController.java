package com.example.purchasebackend.web;

import com.example.purchasebackend.common.ApiResponse;
import com.example.purchasebackend.common.RequestContext;
import com.example.purchasebackend.dto.internal.AdminItemDto;
import com.example.purchasebackend.dto.internal.CreateItemRequest;
import com.example.purchasebackend.dto.internal.UpdateItemRequest;
import com.example.purchasebackend.service.DeveloperAppService;
import com.example.purchasebackend.service.ItemService;
import com.example.purchasebackend.service.mapper.DtoMapper;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal/admin item management. Guarded by {@code InternalAdminTokenFilter}.
 *
 * <p>// TODO: Replace demo seed data with developer portal item creation later.
 */
@RestController
@RequestMapping("/api/v1/internal/items")
public class InternalItemController {

    private final ItemService itemService;
    private final DeveloperAppService developerAppService;

    public InternalItemController(ItemService itemService, DeveloperAppService developerAppService) {
        this.itemService = itemService;
        this.developerAppService = developerAppService;
    }

    /** POST /internal/items — create an item (itemId unique per developer app). */
    @PostMapping
    public ApiResponse<AdminItemDto> create(@Valid @RequestBody CreateItemRequest request) {
        developerAppService.getById(request.developerAppId()); // validates app exists
        AdminItemDto data = DtoMapper.toAdminItemDto(itemService.createItem(request));
        return ApiResponse.ok(data, RequestContext.getRequestId());
    }

    /**
     * PATCH /internal/items/{itemId} — update mutable fields. {@code developerAppId} identifies the
     * owning app. itemId and type are immutable to protect historical purchase records.
     */
    @PatchMapping("/{itemId}")
    public ApiResponse<AdminItemDto> update(@PathVariable String itemId,
                                            @RequestParam String developerAppId,
                                            @RequestBody UpdateItemRequest request) {
        developerAppService.getById(developerAppId);
        AdminItemDto data = DtoMapper.toAdminItemDto(itemService.updateItem(developerAppId, itemId, request));
        return ApiResponse.ok(data, RequestContext.getRequestId());
    }
}
