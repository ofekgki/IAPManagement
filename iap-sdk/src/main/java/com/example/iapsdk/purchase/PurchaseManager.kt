package com.example.iapsdk.purchase

import com.example.iapsdk.analytics.AnalyticsTracker
import com.example.iapsdk.billing.BillingProvider
import com.example.iapsdk.config.BillingMode
import com.example.iapsdk.error.ErrorMapper
import com.example.iapsdk.models.PurchaseException
import com.example.iapsdk.models.PurchaseItem
import com.example.iapsdk.models.PurchaseResult
import com.example.iapsdk.models.PurchaseSdkError

/**
 * Orchestrates the purchase flow on top of a [BillingProvider]. It **chooses the provider from the
 * active [BillingMode]** ([MockBillingProvider] vs [GooglePlayBillingProvider]) and delegates
 * fulfillment to it, while keeping cross-cutting concerns — analytics events and error normalization
 * — here. Both the public `makePurchase` and the popup's Confirm funnel through this class.
 */
internal class PurchaseManager(
    billingMode: BillingMode,
    mockProvider: BillingProvider,
    googlePlayProvider: BillingProvider,
    private val analytics: AnalyticsTracker,
    private val userId: String?,
) {

    /** The provider selected for this session, based on the chosen [BillingMode]. */
    private val billingProvider: BillingProvider = when (billingMode) {
        BillingMode.MOCK -> mockProvider
        BillingMode.GOOGLE_PLAY -> googlePlayProvider
    }

    /** Loads an item via the provider; raises [PurchaseSdkError.ItemNotFound] when absent. */
    suspend fun loadItem(itemId: String): PurchaseItem {
        PurchaseValidator.validateItemId(itemId)
        return billingProvider.getItem(itemId)
            ?: throw PurchaseException(PurchaseSdkError.ItemNotFound)
    }

    /** Loads the full catalog via the provider. */
    suspend fun loadItems(): List<PurchaseItem> = billingProvider.getItems()

    /** Runs a purchase end-to-end, emitting start/success/failure analytics around the provider. */
    suspend fun makePurchase(itemId: String, paymentMethodId: String? = null): PurchaseResult {
        // paymentMethodId carries the user's chosen payment method (APPLE_PAY / GOOGLE_PLAY / PAYPAL
        // / CREDIT_CARD), recorded by the backend for the revenue-by-payment-method breakdown.
        analytics.trackPurchaseStarted(itemId)
        return try {
            val result = billingProvider.makePurchase(itemId, userId, paymentMethodId)
            analytics.trackPurchaseSuccess(itemId, result.purchaseId)
            result
        } catch (t: Throwable) {
            val error = handlePurchaseFailure(t)
            analytics.trackPurchaseFailed(itemId, error.code)
            throw PurchaseException(error)
        }
    }

    /**
     * Restores prior purchases, emitting restore_started/success/failed analytics. When [itemId] is
     * non-null, only that single item is returned (per-item return); when null, all owned items are.
     */
    suspend fun restorePurchases(itemId: String? = null): List<PurchaseResult> {
        analytics.trackRestoreStarted()
        return try {
            val results = billingProvider.restorePurchases(userId, itemId)
            analytics.trackRestoreSuccess()
            results
        } catch (t: Throwable) {
            val error = handlePurchaseFailure(t)
            analytics.trackRestoreFailed(error.code)
            throw PurchaseException(error)
        }
    }

    /** Normalizes a failure into a [PurchaseSdkError] (analytics is emitted by the caller). */
    fun handlePurchaseFailure(throwable: Throwable): PurchaseSdkError =
        ErrorMapper.mapThrowableToSdkError(throwable)
}
