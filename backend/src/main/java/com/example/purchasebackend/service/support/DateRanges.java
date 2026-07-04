package com.example.purchasebackend.service.support;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/** Resolves optional from/to date query params into an Instant range. Default start: 1 Jan 2026. */
public final class DateRanges {

    /**
     * Default window start when no {@code from} is given: the beginning of the demo history
     * (1 Jan 2026), so every portal view spans the full dataset by default.
     */
    private static final LocalDate DEFAULT_START = LocalDate.of(2026, 1, 1);

    private DateRanges() {
    }

    public static Instant resolveFrom(LocalDate from) {
        LocalDate start = from != null ? from : DEFAULT_START;
        return start.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    public static Instant resolveTo(LocalDate to) {
        // Exclusive upper bound: end of the given day, or "now + 1 minute" when unset.
        return to != null
                ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
                : Instant.now().plusSeconds(60);
    }
}
