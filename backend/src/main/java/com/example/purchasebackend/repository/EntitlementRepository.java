package com.example.purchasebackend.repository;

import com.example.purchasebackend.domain.Entitlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EntitlementRepository extends JpaRepository<Entitlement, String> {

    List<Entitlement> findByDeveloperAppIdAndUserId(String developerAppId, String userId);

    List<Entitlement> findByDeveloperAppId(String developerAppId);

    long countByDeveloperAppIdAndStatus(String developerAppId,
            com.example.purchasebackend.domain.enums.EntitlementStatus status);

    List<Entitlement> findByDeveloperAppIdAndUserIdAndEntitlementId(
            String developerAppId, String userId, String entitlementId);

    Optional<Entitlement> findFirstByDeveloperAppIdAndUserIdAndEntitlementIdOrderByCreatedAtDesc(
            String developerAppId, String userId, String entitlementId);
}
