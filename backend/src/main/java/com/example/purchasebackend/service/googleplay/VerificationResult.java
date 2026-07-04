package com.example.purchasebackend.service.googleplay;

import java.time.Instant;

/**
 * Outcome of a Google Play verification attempt.
 *
 * <p>{@code outcome} distinguishes "not configured" (the only thing that can happen today) from a
 * real verified/failed result, so the purchase flow can return the correct SDK error.
 */
public record VerificationResult(Outcome outcome, String message, Instant subscriptionExpiry) {

    public enum Outcome {
        /** Verification is not implemented/configured. Fail safely; never grant. */
        NOT_CONFIGURED,
        /** The token was verified and the purchase is valid. */
        VERIFIED,
        /** The token was checked and is invalid/refunded/etc. */
        FAILED
    }

    public static VerificationResult notConfigured() {
        return new VerificationResult(Outcome.NOT_CONFIGURED,
                "Google Play purchase verification is not configured yet.", null);
    }

    public static VerificationResult verified(Instant subscriptionExpiry) {
        return new VerificationResult(Outcome.VERIFIED, "Verified.", subscriptionExpiry);
    }

    public static VerificationResult failed(String message) {
        return new VerificationResult(Outcome.FAILED, message, null);
    }
}
