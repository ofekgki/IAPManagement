package com.example.purchasebackend.service;

import com.example.purchasebackend.common.ApiException;
import com.example.purchasebackend.common.ErrorCode;
import com.example.purchasebackend.common.Ids;
import com.example.purchasebackend.domain.PurchaseItem;
import com.example.purchasebackend.domain.enums.ItemType;
import com.example.purchasebackend.dto.internal.CreateItemRequest;
import com.example.purchasebackend.dto.internal.UpdateItemRequest;
import com.example.purchasebackend.repository.PurchaseItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Item/product management.
 *
 * <p>// TODO: In GOOGLE_PLAY mode, fetch ProductDetails from Google Play Billing through the client
 * SDK, not from this backend directly. Prices stored here are MOCK/demo display values.
 */
@Service
public class ItemService {

    private static final Logger log = LoggerFactory.getLogger(ItemService.class);

    private final PurchaseItemRepository itemRepository;

    public ItemService(PurchaseItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public List<PurchaseItem> listActiveItems(String developerAppId) {
        log.debug("item list requested app={}", developerAppId);
        return itemRepository.findByDeveloperAppIdAndIsActiveTrue(developerAppId);
    }

    /** Looks up an item by id regardless of active state (used for display, e.g. restore). */
    public Optional<PurchaseItem> findItem(String developerAppId, String itemId) {
        return itemRepository.findByDeveloperAppIdAndItemId(developerAppId, itemId);
    }

    /** Returns an active item or throws ITEM_NOT_FOUND. */
    public PurchaseItem getActiveItem(String developerAppId, String itemId) {
        PurchaseItem item = itemRepository.findByDeveloperAppIdAndItemId(developerAppId, itemId)
                .orElseThrow(() -> new ApiException(ErrorCode.ITEM_NOT_FOUND));
        if (!item.isActive()) {
            throw new ApiException(ErrorCode.ITEM_NOT_FOUND);
        }
        log.debug("item requested app={} item={}", developerAppId, itemId);
        return item;
    }

    public PurchaseItem createItem(CreateItemRequest req) {
        if (itemRepository.existsByDeveloperAppIdAndItemId(req.developerAppId(), req.itemId())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST,
                    "itemId already exists for this developer app: " + req.itemId());
        }
        PurchaseItem item = new PurchaseItem();
        item.setId(Ids.newId("item"));
        item.setDeveloperAppId(req.developerAppId());
        item.setItemId(req.itemId());
        item.setName(req.name());
        item.setDescription(req.description());
        item.setType(parseType(req.type()));
        item.setPriceDisplay(req.priceDisplay());
        item.setCurrency(req.currency());
        item.setGooglePlayProductId(req.googlePlayProductId());
        item.setEntitlementId(req.entitlementId());
        item.setActive(req.isActive() == null || req.isActive());
        return itemRepository.save(item);
    }

    /** Patches mutable fields only. itemId and type are immutable to protect purchase history. */
    public PurchaseItem updateItem(String developerAppId, String itemId, UpdateItemRequest req) {
        PurchaseItem item = itemRepository.findByDeveloperAppIdAndItemId(developerAppId, itemId)
                .orElseThrow(() -> new ApiException(ErrorCode.ITEM_NOT_FOUND));
        if (req.name() != null) {
            item.setName(req.name());
        }
        if (req.description() != null) {
            item.setDescription(req.description());
        }
        if (req.priceDisplay() != null) {
            item.setPriceDisplay(req.priceDisplay());
        }
        if (req.currency() != null) {
            item.setCurrency(req.currency());
        }
        if (req.googlePlayProductId() != null) {
            item.setGooglePlayProductId(req.googlePlayProductId());
        }
        if (req.entitlementId() != null) {
            item.setEntitlementId(req.entitlementId());
        }
        if (req.isActive() != null) {
            item.setActive(req.isActive());
        }
        return itemRepository.save(item);
    }

    private ItemType parseType(String raw) {
        try {
            return ItemType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Invalid item type: " + raw);
        }
    }
}
