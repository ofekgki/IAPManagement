package com.example.purchasebackend.repository;

import com.example.purchasebackend.domain.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, String> {

    Optional<IdempotencyRecord> findByDeveloperAppIdAndIdempotencyKey(String developerAppId, String idempotencyKey);
}
