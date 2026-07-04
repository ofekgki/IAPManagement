package com.example.iapsdk.analytics

import android.util.Log
import com.example.iapsdk.api.ApiClient
import com.example.iapsdk.api.request.AnalyticsEventRequest
import com.example.iapsdk.config.BillingMode
import com.example.iapsdk.config.PurchaseSdkConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Client-side analytics abstraction. Named `track*` helpers describe SDK lifecycle events; each is
 * tagged with the active [BillingMode] (`billingMode = MOCK | GOOGLE_PLAY`) so funnels can be split
 * by mode. Events are fire-and-forget: launched on [scope], failures swallowed (analytics must never
 * break a purchase).
 *
 * For now events are logged locally with [Log] and forwarded to the mock [ApiClient]. When
 * [PurchaseSdkConfig.enableAnalytics] is false, every method is a no-op.
 *
 * TODO(backend): forward these events to a real analytics backend / pipeline.
 */
internal class AnalyticsTracker(
    private val apiClient: ApiClient,
    private val config: PurchaseSdkConfig,
    private val billingMode: BillingMode,
    private val userId: String?,
    private val scope: CoroutineScope,
) {

    private val enabled: Boolean get() = config.enableAnalytics

    fun trackSdkInitialized() = track("sdk_initialized")

    fun trackItemRequested(itemId: String) = track("item_requested", "item_id" to itemId)

    fun trackPopupShown(itemId: String) = track("purchase_popup_shown", "item_id" to itemId)

    fun trackPopupConfirmed(itemId: String) = track("purchase_confirm_clicked", "item_id" to itemId)

    fun trackPopupCancelled(itemId: String) = track("purchase_cancel_clicked", "item_id" to itemId)

    fun trackPurchaseStarted(itemId: String) = track("purchase_started", "item_id" to itemId)

    fun trackPurchaseSuccess(itemId: String, purchaseId: String) =
        track("purchase_success", "item_id" to itemId, "purchase_id" to purchaseId)

    fun trackPurchaseFailed(itemId: String, errorCode: String) =
        track("purchase_failed", "item_id" to itemId, "error_code" to errorCode)

    fun trackRestoreStarted() = track("restore_started")

    fun trackRestoreSuccess() = track("restore_success")

    fun trackRestoreFailed(errorCode: String) = track("restore_failed", "error_code" to errorCode)

    fun trackEntitlementsListed() = track("entitlements_listed")

    fun trackHasEntitlementChecked(itemId: String, result: Boolean) =
        track("has_entitlement_checked", "item_id" to itemId, "result" to result)

    fun trackCustomEvent(eventName: String, properties: Map<String, Any>) =
        track(eventName, properties)

    /** Suspends until the event has been handed to the API (used internally / testable). */
    suspend fun sendEvent(eventName: String, properties: Map<String, Any>) {
        if (!enabled) return
        val request = AnalyticsEventRequest(
            eventName = eventName,
            userId = userId,
            properties = withBillingMode(properties),
            timestamp = System.currentTimeMillis(),
        )
        // TODO(backend): POST to a real analytics endpoint instead of the mock ApiClient.
        runCatching { apiClient.sendAnalyticsEvent(request) }
            .onFailure { if (config.enableLogs) Log.w(TAG, "Failed to send analytics event", it) }
    }

    private fun track(eventName: String, vararg properties: Pair<String, Any>) =
        track(eventName, properties.toMap())

    private fun track(eventName: String, properties: Map<String, Any>) {
        if (!enabled) return
        val enriched = withBillingMode(properties)
        if (config.enableLogs) Log.d(TAG, "track: $eventName $enriched")
        scope.launch { sendEvent(eventName, properties) }
    }

    /** Every event carries the active billing mode. */
    private fun withBillingMode(properties: Map<String, Any>): Map<String, Any> =
        properties + ("billingMode" to billingMode.name)

    private companion object {
        const val TAG = "IapAnalytics"
    }
}
