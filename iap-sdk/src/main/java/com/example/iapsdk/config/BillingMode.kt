package com.example.iapsdk.config

/**
 * Selects how the SDK fulfills purchases. Chosen at [com.example.iapsdk.PurchaseSdk.init] time.
 *
 * - [MOCK] — a fully local, simulated billing flow. No Google Play Console products, no
 *   `BillingClient`, no payment. Ideal for demos, tests, and this educational project. This is the
 *   **default** for now.
 * - [GOOGLE_PLAY] — the real Google Play Billing flow. The scaffold exists but real billing is not
 *   wired up yet; calls fail gracefully with `PurchaseSdkError.GooglePlayNotConfigured` until the
 *   `TODO`s in `GooglePlayBillingProvider` are implemented.
 */
enum class BillingMode {
    GOOGLE_PLAY,
    MOCK,
}
