package com.example.purchasebackend.service.support;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/** Resolves optional from/to date query params into an Instant range. Default start: 1 Jan of the current year. */
public final class DateRanges {

    private DateRanges() {
    }

    /**
     * Default window start when no {@code from} is given: the first day of the current calendar year,
     * so every portal view spans year-to-date by default.
     */
    private static LocalDate defaultStart() {
        return LocalDate.now(ZoneOffset.UTC).withDayOfYear(1);
    }

    public static Instant resolveFrom(LocalDate from) {
        LocalDate start = from != null ? from : defaultStart();
        return start.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    public static Instant resolveTo(LocalDate to) {
        // Exclusive upper bound: end of the given day, or "now + 1 minute" when unset.
        return to != null
                ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
                : Instant.now().plusSeconds(60);
    }
}
