package com.example.purchasebackend.service.support;

import com.example.purchasebackend.common.ApiException;
import com.example.purchasebackend.common.ErrorCode;
import com.example.purchasebackend.domain.enums.BillingMode;

/** Lenient parsing of the client-supplied {@code billingMode} string into the enum. */
public final class BillingModes {

    private BillingModes() {
    }

    /** Parses {@code raw} into a mode, or returns null when blank (for optional filters). */
    public static BillingMode parseOrNull(String raw) {
        return (raw == null || raw.isBlank()) ? null : parseOrDefault(raw, null);
    }

    /** Parses {@code raw}; falls back to {@code fallback} when blank; throws on an unknown value. */
    public static BillingMode parseOrDefault(String raw, BillingMode fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return BillingMode.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.BILLING_MODE_NOT_SUPPORTED,
                    "Unsupported billing mode: \"" + raw + "\".");
        }
    }
}
