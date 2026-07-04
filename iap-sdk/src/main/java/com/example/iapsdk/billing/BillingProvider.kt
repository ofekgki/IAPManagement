package com.example.iapsdk.billing

import com.example.iapsdk.models.PurchaseItem
import com.example.iapsdk.models.PurchaseResult

/**
 * The billing strategy seam. One implementation per [com.example.iapsdk.config.BillingMode]:
 * [MockBillingProvider] (local simulation) and [GooglePlayBillingProvider] (real Play Billing,
 * scaffolded). `PurchaseManager` holds whichever one was selected at `init` and delegates to it, so
 * the rest of the SDK is billing-mode agnostic.
 *
 * Implementations are responsible for the *fulfillment* of a purchase (and, on success, granting the
 * entitlement). Cross-cutting concerns — analytics, error normalization, validation — stay in
 * `PurchaseManager`.
 */
internal interface BillingProvider {

    /** Returns the item for [itemId], or `null` if it doesn't exist. May throw for provider errors. */
    suspend fun getItem(itemId: String): PurchaseItem?

    /** Returns the full catalog of available items. May throw for provider errors. */
    suspend fun getItems(): List<PurchaseItem>

    /**
     * Performs a purchase of [itemId] for [userId] and returns its result. [paymentMethod] is the
     * chosen method's API value (APPLE_PAY / GOOGLE_PLAY / PAYPAL / CREDIT_CARD), or null for default.
     */
    suspend fun makePurchase(itemId: String, userId: String?, paymentMethod: String? = null): PurchaseResult

    /**
     * Returns the user's previously completed purchases. When [itemId] is non-null, only that single
     * item is restored/returned (per-item return); when null, all owned items are restored.
     */
    suspend fun restorePurchases(userId: String?, itemId: String? = null): List<PurchaseResult>
}
