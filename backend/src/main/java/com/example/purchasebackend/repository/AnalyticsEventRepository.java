package com.example.purchasebackend.repository;

import com.example.purchasebackend.domain.AnalyticsEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, String> {

    long countByDeveloperAppIdAndEventNameAndCreatedAtBetween(
            String developerAppId, String eventName, Instant from, Instant to);

    List<AnalyticsEvent> findByDeveloperAppIdAndCreatedAtBetween(
            String developerAppId, Instant from, Instant to);

    List<AnalyticsEvent> findTop200ByDeveloperAppIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            String developerAppId, Instant from, Instant to);

    List<AnalyticsEvent> findByPurchaseId(String purchaseId);
}
