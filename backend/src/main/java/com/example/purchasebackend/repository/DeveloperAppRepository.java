package com.example.purchasebackend.repository;

import com.example.purchasebackend.domain.DeveloperApp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeveloperAppRepository extends JpaRepository<DeveloperApp, String> {

    List<DeveloperApp> findByOwnerUserIdOrderByCreatedAtDesc(String ownerUserId);

    Optional<DeveloperApp> findByIdAndOwnerUserId(String id, String ownerUserId);
}
