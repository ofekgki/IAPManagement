package com.example.purchasebackend.service;

import com.example.purchasebackend.common.Ids;
import com.example.purchasebackend.domain.AnalyticsEvent;
import com.example.purchasebackend.domain.enums.BillingMode;
import com.example.purchasebackend.dto.internal.AnalyticsSummaryResponse;
import com.example.purchasebackend.repository.AnalyticsEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * Collects analytics events.
 *
 * <p>Analytics must never break the purchase flow — {@link #record} swallows and logs storage
 * failures instead of throwing.
 *
 * <p>// TODO: Add aggregation for dashboard analytics later.
 * <p>// TODO: Add privacy controls and event retention policy.
 */
@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final AnalyticsEventRepository repository;
    private final ObjectMapper objectMapper;

    public AnalyticsService(AnalyticsEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /** Best-effort store of an event. Returns true if stored, false if it was swallowed. */
    public boolean record(String developerAppId, String userId, String eventName, BillingMode billingMode,
                          String itemId, String purchaseId, Map<String, Object> metadata) {
        try {
            AnalyticsEvent event = new AnalyticsEvent();
            event.setId(Ids.newId("evt"));
            event.setDeveloperAppId(developerAppId);
            event.setUserId(userId);
            event.setEventName(eventName);
            event.setBillingMode(billingMode);
            event.setItemId(itemId);
            event.setPurchaseId(purchaseId);
            event.setMetadataJson(metadata == null ? null : objectMapper.writeValueAsString(metadata));
            repository.save(event);
            log.debug("analytics event received name={} app={}", eventName, developerAppId);
            return true;
        } catch (Exception ex) {
            log.warn("Failed to store analytics event name={} app={}", eventName, developerAppId, ex);
            return false;
        }
    }

    public AnalyticsSummaryResponse summary(String developerAppId, Instant from, Instant to) {
        return new AnalyticsSummaryResponse(
                count(developerAppId, "purchase_started", from, to),
                count(developerAppId, "purchase_success", from, to),
                count(developerAppId, "purchase_failed", from, to),
                count(developerAppId, "purchase_popup_shown", from, to),
                count(developerAppId, "restore_started", from, to));
    }

    private long count(String developerAppId, String eventName, Instant from, Instant to) {
        return repository.countByDeveloperAppIdAndEventNameAndCreatedAtBetween(developerAppId, eventName, from, to);
    }
}
