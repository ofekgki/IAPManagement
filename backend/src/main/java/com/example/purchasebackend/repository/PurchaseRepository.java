package com.example.purchasebackend.repository;

import com.example.purchasebackend.domain.Purchase;
import com.example.purchasebackend.domain.enums.PurchaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, String> {

    Optional<Purchase> findByIdAndDeveloperAppId(String id, String developerAppId);

    List<Purchase> findByDeveloperAppIdAndUserIdAndStatus(String developerAppId, String userId, PurchaseStatus status);

    List<Purchase> findByDeveloperAppIdAndUserId(String developerAppId, String userId);

    List<Purchase> findByDeveloperAppId(String developerAppId);

    List<Purchase> findByDeveloperAppIdAndStatus(String developerAppId, PurchaseStatus status);

    /**
     * Analytics window query ([from, to) on completedAt) pushed down to the DB — served by
     * idx_purchase_app_status_completed instead of loading the app's full purchase history.
     */
    List<Purchase> findByDeveloperAppIdAndStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(
            String developerAppId, PurchaseStatus status, Instant from, Instant to);

    /**
     * Portal listing window query ([from, to) on createdAt) pushed down to the DB — served by
     * idx_purchase_app_created.
     */
    List<Purchase> findByDeveloperAppIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            String developerAppId, Instant from, Instant to);
}
