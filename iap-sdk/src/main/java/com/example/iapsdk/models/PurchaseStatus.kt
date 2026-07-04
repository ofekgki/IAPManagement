package com.example.iapsdk.models

/**
 * Lifecycle state of a single purchase, as reported back to the client app.
 */
enum class PurchaseStatus {
    /** The purchase completed and the entitlement was granted. */
    SUCCESS,

    /** The purchase was accepted but is not yet final (e.g. awaiting payment capture). */
    PENDING,

    /** The purchase attempt failed. See [com.example.iapsdk.models.PurchaseResult.message]. */
    FAILED,

    /** The user backed out before the purchase completed. */
    CANCELLED,

    /** A prior purchase re-granted to the user via [com.example.iapsdk.PurchaseSdk.restorePurchases]. */
    RESTORED,
}
