package com.example.purchasebackend.web.portal;

import com.example.purchasebackend.common.ApiResponse;
import com.example.purchasebackend.common.RequestContext;
import com.example.purchasebackend.dto.portal.ItemDtos.CreateItemRequest;
import com.example.purchasebackend.dto.portal.ItemDtos.PortalItemDto;
import com.example.purchasebackend.dto.portal.ItemDtos.UpdateItemRequest;
import com.example.purchasebackend.security.PortalContext;
import com.example.purchasebackend.service.portal.PortalAppService;
import com.example.purchasebackend.service.portal.PortalItemService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Purchase item management for an app (JWT). itemId is the SDK-facing product id. */
@RestController
@RequestMapping("/api/v1/portal/apps/{appId}/items")
public class PortalItemController {

    private final PortalItemService itemService;
    private final PortalAppService appService;

    public PortalItemController(PortalItemService itemService, PortalAppService appService) {
        this.itemService = itemService;
        this.appService = appService;
    }

    @GetMapping
    public ApiResponse<List<PortalItemDto>> list(@PathVariable String appId) {
        authorize(appId);
        return ApiResponse.ok(itemService.list(appId), RequestContext.getRequestId());
    }

    @PostMapping
    public ApiResponse<PortalItemDto> create(@PathVariable String appId,
                                             @Valid @RequestBody CreateItemRequest request) {
        authorize(appId);
        return ApiResponse.ok(itemService.create(appId, request), RequestContext.getRequestId());
    }

    @GetMapping("/{itemId}")
    public ApiResponse<PortalItemDto> get(@PathVariable String appId, @PathVariable String itemId) {
        authorize(appId);
        return ApiResponse.ok(itemService.get(appId, itemId), RequestContext.getRequestId());
    }

    @PatchMapping("/{itemId}")
    public ApiResponse<PortalItemDto> update(@PathVariable String appId, @PathVariable String itemId,
                                             @RequestBody UpdateItemRequest request) {
        authorize(appId);
        return ApiResponse.ok(itemService.update(appId, itemId, request), RequestContext.getRequestId());
    }

    @PostMapping("/{itemId}/disable")
    public ApiResponse<PortalItemDto> disable(@PathVariable String appId, @PathVariable String itemId) {
        authorize(appId);
        return ApiResponse.ok(itemService.setActive(appId, itemId, false), RequestContext.getRequestId());
    }

    @PostMapping("/{itemId}/enable")
    public ApiResponse<PortalItemDto> enable(@PathVariable String appId, @PathVariable String itemId) {
        authorize(appId);
        return ApiResponse.ok(itemService.setActive(appId, itemId, true), RequestContext.getRequestId());
    }

    private void authorize(String appId) {
        appService.requireOwnedApp(appId, PortalContext.requireUser());
    }
}
