// Shared portal constants.

/**
 * Currencies offered anywhere the portal lets you choose one (e.g. the New Item form). Keep this the
 * single source of truth so currency pickers stay consistent across the project.
 *
 * NOTE: revenue analytics currently sum minor units across currencies without conversion (see the
 * multi-currency TODO), so for meaningful revenue numbers keep an app's items on one currency.
 */
export const CURRENCIES = ["ILS", "GBP", "USD", "EUR"] as const;

export type Currency = (typeof CURRENCIES)[number];

/** Default selection for new currency pickers. */
export const DEFAULT_CURRENCY: Currency = "USD";
