package com.example.purchasebackend.web;

import com.example.purchasebackend.common.ApiResponse;
import com.example.purchasebackend.common.RequestContext;
import com.example.purchasebackend.domain.DeveloperApp;
import com.example.purchasebackend.dto.sdk.ItemDto;
import com.example.purchasebackend.dto.sdk.ItemsResponse;
import com.example.purchasebackend.service.ItemService;
import com.example.purchasebackend.service.mapper.DtoMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** SDK item endpoints. */
@RestController
@RequestMapping("/api/v1/sdk/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    /**
     * GET /api/v1/sdk/items — active items for the app. {@code billingMode} is accepted for parity
     * with the SDK but does not change the MOCK result.
     *
     * <p>// TODO: In GOOGLE_PLAY mode, fetch ProductDetails from Google Play Billing through the
     * client SDK, not from this backend directly.
     */
    @GetMapping
    public ApiResponse<ItemsResponse> list(@RequestParam(required = false) String billingMode) {
        DeveloperApp app = RequestContext.requireDeveloperApp();
        List<ItemDto> items = itemService.listActiveItems(app.getId()).stream()
                .map(DtoMapper::toItemDto)
                .toList();
        return ApiResponse.ok(new ItemsResponse(items), RequestContext.getRequestId());
    }

    /** GET /api/v1/sdk/items/{itemId} — a single active item (ITEM_NOT_FOUND otherwise). */
    @GetMapping("/{itemId}")
    public ApiResponse<ItemDto> get(@PathVariable String itemId) {
        DeveloperApp app = RequestContext.requireDeveloperApp();
        ItemDto dto = DtoMapper.toItemDto(itemService.getActiveItem(app.getId(), itemId));
        return ApiResponse.ok(dto, RequestContext.getRequestId());
    }
}
