package com.example.purchasebackend.service.support;

import com.example.purchasebackend.common.ApiException;
import com.example.purchasebackend.common.ErrorCode;
import com.example.purchasebackend.domain.enums.PaymentMethod;

/** Lenient parsing of the client-supplied {@code paymentMethod} string into the enum. */
public final class PaymentMethods {

    /** Used when a client starts a purchase without naming a method. */
    public static final PaymentMethod DEFAULT = PaymentMethod.CREDIT_CARD;

    private PaymentMethods() {
    }

    /** Parses {@code raw} into a method, or returns null when blank (for optional filters). */
    public static PaymentMethod parseOrNull(String raw) {
        return (raw == null || raw.isBlank()) ? null : parseOrDefault(raw, null);
    }

    /** Parses {@code raw}; falls back to {@code fallback} when blank; throws on an unknown value. */
    public static PaymentMethod parseOrDefault(String raw, PaymentMethod fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return PaymentMethod.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Unsupported payment method: \"" + raw + "\".");
        }
    }
}
