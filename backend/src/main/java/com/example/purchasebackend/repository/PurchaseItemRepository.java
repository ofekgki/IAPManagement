package com.example.purchasebackend.repository;

import com.example.purchasebackend.domain.PurchaseItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, String> {

    Optional<PurchaseItem> findByDeveloperAppIdAndItemId(String developerAppId, String itemId);

    List<PurchaseItem> findByDeveloperAppIdAndIsActiveTrue(String developerAppId);

    List<PurchaseItem> findByDeveloperAppId(String developerAppId);

    boolean existsByDeveloperAppIdAndItemId(String developerAppId, String itemId);

    /** Resolves the item that grants a given entitlement (used by entitlement checks). */
    Optional<PurchaseItem> findFirstByDeveloperAppIdAndEntitlementId(String developerAppId, String entitlementId);
}
