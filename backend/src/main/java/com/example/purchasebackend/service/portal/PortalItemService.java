package com.example.purchasebackend.service.portal;

import com.example.purchasebackend.common.ApiException;
import com.example.purchasebackend.common.ErrorCode;
import com.example.purchasebackend.common.Ids;
import com.example.purchasebackend.domain.PurchaseItem;
import com.example.purchasebackend.domain.enums.ItemType;
import com.example.purchasebackend.dto.portal.ItemDtos.CreateItemRequest;
import com.example.purchasebackend.dto.portal.ItemDtos.PortalItemDto;
import com.example.purchasebackend.dto.portal.ItemDtos.UpdateItemRequest;
import com.example.purchasebackend.repository.PurchaseItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Portal item management: list/create/get/update/enable/disable, with per-app unique item ids and
 * auto-generation of item/entitlement ids.
 */
@Service
public class PortalItemService {

    private final PurchaseItemRepository itemRepository;

    public PortalItemService(PurchaseItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public List<PortalItemDto> list(String appId) {
        return itemRepository.findByDeveloperAppId(appId).stream().map(PortalItemService::toDto).toList();
    }

    public PortalItemDto get(String appId, String itemId) {
        return toDto(getEntity(appId, itemId));
    }

    public PurchaseItem getEntity(String appId, String itemId) {
        return itemRepository.findByDeveloperAppIdAndItemId(appId, itemId)
                .orElseThrow(() -> new ApiException(ErrorCode.ITEM_NOT_FOUND));
    }

    public PortalItemDto create(String appId, CreateItemRequest req) {
        ItemType type = parseType(req.type());
        String itemId = normalizeSnakeCase(
                req.itemId() != null && !req.itemId().isBlank() ? req.itemId() : req.name());
        if (itemId.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Could not derive a valid itemId.");
        }
        if (itemRepository.existsByDeveloperAppIdAndItemId(appId, itemId)) {
            throw new ApiException(ErrorCode.ITEM_ID_ALREADY_EXISTS);
        }

        String entitlementId = req.entitlementId();
        if ((entitlementId == null || entitlementId.isBlank()) && type != ItemType.CONSUMABLE) {
            entitlementId = "ent_" + itemId; // auto-generate for non-consumable / subscription
        }
        if (type != ItemType.CONSUMABLE && (entitlementId == null || entitlementId.isBlank())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST,
                    "entitlementId is required for NON_CONSUMABLE and SUBSCRIPTION items.");
        }

        PurchaseItem item = new PurchaseItem();
        item.setId(Ids.newId("item"));
        item.setDeveloperAppId(appId);
        item.setItemId(itemId);
        item.setName(req.name());
        item.setDescription(req.description());
        item.setType(type);
        item.setEntitlementId(entitlementId);
        item.setPriceAmountMinor(req.priceAmountMinor() == null ? 0L : req.priceAmountMinor());
        item.setCurrency(req.currency());
        item.setPriceDisplay(req.priceDisplay() != null && !req.priceDisplay().isBlank()
                ? req.priceDisplay()
                : derivePriceDisplay(item.getPriceAmountMinor(), req.currency()));
        item.setGooglePlayProductId(req.googlePlayProductId());
        item.setActive(req.isActive() == null || req.isActive());
        return toDto(itemRepository.save(item));
    }

    public PortalItemDto update(String appId, String itemId, UpdateItemRequest req) {
        PurchaseItem item = getEntity(appId, itemId);
        if (req.name() != null) {
            item.setName(req.name());
        }
        if (req.description() != null) {
            item.setDescription(req.description());
        }
        if (req.entitlementId() != null) {
            item.setEntitlementId(req.entitlementId());
        }
        if (req.priceAmountMinor() != null) {
            item.setPriceAmountMinor(req.priceAmountMinor());
        }
        if (req.currency() != null) {
            item.setCurrency(req.currency());
        }
        if (req.priceDisplay() != null) {
            item.setPriceDisplay(req.priceDisplay());
        }
        if (req.googlePlayProductId() != null) {
            item.setGooglePlayProductId(req.googlePlayProductId());
        }
        if (req.isActive() != null) {
            item.setActive(req.isActive());
        }
        return toDto(itemRepository.save(item));
    }

    public PortalItemDto setActive(String appId, String itemId, boolean active) {
        PurchaseItem item = getEntity(appId, itemId);
        item.setActive(active);
        return toDto(itemRepository.save(item));
    }

    // --- helpers -----------------------------------------------------------------------------

    static String normalizeSnakeCase(String raw) {
        return raw.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private static String derivePriceDisplay(long minor, String currency) {
        double major = minor / 100.0;
        String symbol = "USD".equalsIgnoreCase(currency) ? "$" : "";
        return symbol + String.format("%.2f", major) + (symbol.isEmpty() ? " " + currency : "");
    }

    private ItemType parseType(String raw) {
        try {
            return ItemType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Invalid item type: " + raw);
        }
    }

    static PortalItemDto toDto(PurchaseItem i) {
        return new PortalItemDto(
                i.getId(), i.getItemId(), i.getName(), i.getDescription(), i.getType().name(),
                i.getEntitlementId(), i.getPriceAmountMinor(), i.getCurrency(), i.getPriceDisplay(),
                i.getGooglePlayProductId(), i.isActive(), i.getCreatedAt(), i.getUpdatedAt());
    }
}
