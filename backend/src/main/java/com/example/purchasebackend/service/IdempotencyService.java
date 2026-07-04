package com.example.purchasebackend.service;

import com.example.purchasebackend.common.Ids;
import com.example.purchasebackend.domain.IdempotencyRecord;
import com.example.purchasebackend.repository.IdempotencyRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Stores and replays the result of idempotent operations (purchase confirmation), keyed by
 * (developerAppId, Idempotency-Key). A repeated request returns the original serialized response.
 */
@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private final IdempotencyRecordRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRecordRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /** Returns the previously stored response of the given type, if this key was already processed. */
    public <T> Optional<T> findStored(String developerAppId, String idempotencyKey, Class<T> type) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        return repository.findByDeveloperAppIdAndIdempotencyKey(developerAppId, idempotencyKey)
                .map(record -> deserialize(record.getResponseJson(), type));
    }

    /** Persists the response for this key so future retries replay it. No-op if key is blank. */
    public void store(String developerAppId, String idempotencyKey, String purchaseId, Object response) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        // If a concurrent request already stored it, keep the first result.
        if (repository.findByDeveloperAppIdAndIdempotencyKey(developerAppId, idempotencyKey).isPresent()) {
            return;
        }
        try {
            IdempotencyRecord record = new IdempotencyRecord();
            record.setId(Ids.newId("idem"));
            record.setDeveloperAppId(developerAppId);
            record.setIdempotencyKey(idempotencyKey);
            record.setPurchaseId(purchaseId);
            record.setResponseJson(objectMapper.writeValueAsString(response));
            repository.save(record);
        } catch (Exception ex) {
            // Never fail the purchase because idempotency bookkeeping failed; just log it.
            log.warn("Failed to store idempotency record key={}", idempotencyKey, ex);
        }
    }

    private <T> T deserialize(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            log.warn("Failed to deserialize idempotency response", ex);
            return null;
        }
    }
}
