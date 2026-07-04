package com.example.purchasebackend.repository;

import com.example.purchasebackend.domain.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, String> {

    Optional<ApiKey> findByKeyHash(String keyHash);

    List<ApiKey> findByDeveloperAppIdOrderByCreatedAtDesc(String developerAppId);

    Optional<ApiKey> findByIdAndDeveloperAppId(String id, String developerAppId);
}
