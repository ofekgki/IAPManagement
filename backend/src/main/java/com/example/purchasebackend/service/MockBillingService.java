package com.example.purchasebackend.service;

import com.example.purchasebackend.domain.Purchase;
import com.example.purchasebackend.domain.enums.PurchaseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Simulated billing for MOCK mode.
 *
 * <p>// MOCK mode is for demo/testing/educational use only.
 * <p>// It must not be used as proof of real payment.
 *
 * <p>Confirming a mock purchase simply marks it SUCCESS — there is no Google token, no Google UI, and
 * no real charge. The caller ({@code PurchaseService}) then grants the entitlement.
 */
@Service
public class MockBillingService {

    private static final Logger log = LoggerFactory.getLogger(MockBillingService.class);

    /** Marks the purchase as a successful mock transaction. Mutates and returns the same instance. */
    public Purchase confirm(Purchase purchase) {
        purchase.setStatus(PurchaseStatus.SUCCESS);
        purchase.setCompletedAt(Instant.now());
        purchase.setProviderOrderId("mock_order_" + purchase.getId());
        purchase.setFailureCode(null);
        purchase.setFailureMessage(null);
        log.debug("mock purchase confirmed purchaseId={}", purchase.getId());
        return purchase;
    }
}
