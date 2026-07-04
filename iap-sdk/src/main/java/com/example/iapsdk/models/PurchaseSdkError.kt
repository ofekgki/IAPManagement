package com.example.iapsdk.models

/**
 * The SDK's closed set of client-facing errors. Every failure — mock or (future) Google Play — is
 * normalized by the internal `ErrorMapper` into one of these, so the host app can `when`-switch on a
 * stable type instead of catching transport-specific exceptions.
 *
 * Each case carries a stable [code] (handy for analytics/logging) and a human-readable [message].
 * Errors are delivered either by throwing a [PurchaseException] (suspend APIs) or via
 * [com.example.iapsdk.listener.PurchaseListener.onPurchaseFailed] (the popup flow).
 */
sealed class PurchaseSdkError {

    /** Stable, machine-readable code. */
    abstract val code: String

    /** Human-readable description, safe to show or log. */
    abstract val message: String

    /** A method was called before [com.example.iapsdk.PurchaseSdk.init]. */
    data object NotInitialized : PurchaseSdkError() {
        override val code = "SDK_NOT_INITIALIZED"
        override val message = "PurchaseSdk is not initialized. Call PurchaseSdk.init(...) first."
    }

    /** No item exists for the requested id. */
    data object ItemNotFound : PurchaseSdkError() {
        override val code = "ITEM_NOT_FOUND"
        override val message = "The requested item was not found."
    }

    /** The billing service is unreachable / not ready (e.g. network down, Play services missing). */
    data object BillingUnavailable : PurchaseSdkError() {
        override val code = "BILLING_UNAVAILABLE"
        override val message = "Billing is currently unavailable. Please try again later."
    }

    /** The user backed out of the purchase. */
    data object PurchaseCancelled : PurchaseSdkError() {
        override val code = "PURCHASE_CANCELLED"
        override val message = "The purchase was cancelled."
    }

    /** The purchase attempt failed for a non-specific reason. */
    data object PurchaseFailed : PurchaseSdkError() {
        override val code = "PURCHASE_FAILED"
        override val message = "The purchase could not be completed."
    }

    /** `GOOGLE_PLAY` mode was selected but real billing has not been configured yet. */
    data object GooglePlayNotConfigured : PurchaseSdkError() {
        override val code = "GOOGLE_PLAY_NOT_CONFIGURED"
        override val message =
            "Google Play Billing is not configured. Complete the GooglePlayBillingProvider TODOs " +
                "(BillingClient setup, Play Console products, backend verification) to enable it."
    }

    /** A purchase token must be verified (e.g. server-side) before the entitlement can be granted. */
    data object VerificationRequired : PurchaseSdkError() {
        override val code = "VERIFICATION_REQUIRED"
        override val message = "Purchase verification is required before granting the entitlement."
    }

    /** Anything not covered above. [message] carries the detail. */
    data class Unknown(override val message: String) : PurchaseSdkError() {
        override val code = "UNKNOWN_ERROR"
    }
}
