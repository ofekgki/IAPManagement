package com.example.iapsdk

import android.app.Activity
import android.content.Context
import com.example.iapsdk.config.BillingMode
import com.example.iapsdk.config.PurchaseSdkConfig
import com.example.iapsdk.core.SdkRuntime
import com.example.iapsdk.error.ErrorMapper
import com.example.iapsdk.listener.PurchaseListener
import com.example.iapsdk.models.PurchaseException
import com.example.iapsdk.models.PurchaseItem
import com.example.iapsdk.models.PurchaseResult
import com.example.iapsdk.models.PurchaseSdkError
import com.example.iapsdk.models.UserEntitlement
import com.example.iapsdk.purchase.PurchaseValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The single public entry point of the SDK. Everything an app developer needs is on this object;
 * all managers, the API client, storage, and the popup view are `internal` and reached only through
 * here.
 *
 * Lifecycle: call [init] once (e.g. from `Application.onCreate`) before any other method. State is
 * held in a single internal [SdkRuntime]; [logout] tears it down. "Initialized" is therefore just
 * "is there a runtime?" ([isInitialized]).
 *
 * Threading: the data methods are `suspend` and safe to call from any coroutine — they hop to a
 * background dispatcher internally. [showPurchasePopup], [trackEvent], and [logout] are
 * main-thread-friendly and return immediately.
 *
 * Errors: failures are normalized to [PurchaseSdkError] and thrown wrapped in a [PurchaseException],
 * so callers can `catch (e: PurchaseException)` and `when`-switch on `e.error`.
 */
object PurchaseSdk {

    @Volatile
    private var runtime: SdkRuntime? = null

    /** Main-thread scope used to load an item before presenting the popup in [showPurchasePopup]. */
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * Initializes the SDK. Must be called before any other method.
     *
     * Validates inputs, stores the API key / user id / config in local storage, builds the internal
     * managers for the chosen [billingMode], and emits an `sdk_initialized` analytics event. Calling
     * [init] again replaces the previous session (useful when switching users or modes).
     *
     * [billingMode] defaults to [BillingMode.MOCK] because this project is still educational/demo
     * focused — mock billing needs no Google Play setup. Pass [BillingMode.GOOGLE_PLAY] to select the
     * (scaffolded) real Play Billing path. [userId] is optional in MOCK mode; a real deployment
     * should pass a stable id.
     *
     * Only `context.applicationContext` is retained, so passing an Activity here does not leak it.
     *
     * @throws PurchaseException ([PurchaseSdkError.Unknown]) if [apiKey] is blank.
     */
    @JvmStatic
    @JvmOverloads
    fun init(
        context: Context,
        apiKey: String,
        billingMode: BillingMode = BillingMode.MOCK,
        userId: String? = null,
        config: PurchaseSdkConfig = PurchaseSdkConfig(),
    ) {
        // TODO(backend): validate the API key (and fetch server-side config) before proceeding.
        //  Today this only guards against an obviously-missing key; do not hardcode real keys.
        PurchaseValidator.validateApiKey(apiKey)

        // Replace any prior session cleanly.
        runtime?.shutdown()

        val newRuntime = SdkRuntime.create(context, apiKey, userId, billingMode, config)
        newRuntime.localStorage.apply {
            saveApiKey(apiKey)
            userId?.let { saveUserId(it) }
            saveConfig(config)
        }
        runtime = newRuntime
        newRuntime.analytics.trackSdkInitialized()
    }

    /** Returns whether [init] has been called (and not torn down by [logout]). */
    @JvmStatic
    fun isInitialized(): Boolean = runtime != null

    /**
     * Returns a single item by id, preferring the local cache (when caching is enabled) before
     * hitting the API.
     *
     * @throws PurchaseException `SDK_NOT_INITIALIZED` if not initialized, `ITEM_NOT_FOUND` if no
     *   such item exists.
     */
    suspend fun getItem(itemId: String): PurchaseItem {
        val rt = requireRuntime()
        rt.analytics.trackItemRequested(itemId)
        return rt.purchaseManager.loadItem(itemId)
    }

    /**
     * Returns the full catalog of available items for the app, fetched from the backend.
     *
     * @throws PurchaseException `SDK_NOT_INITIALIZED` if not initialized, or a transport error
     *   (e.g. `BILLING_UNAVAILABLE`) if the backend can't be reached.
     */
    suspend fun getItems(): List<PurchaseItem> {
        val rt = requireRuntime()
        return rt.purchaseManager.loadItems()
    }

    /**
     * Loads [itemId] and presents the SDK's purchase popup over [activity]. The outcome is delivered
     * to [listener] on the main thread. If the item cannot be loaded, [PurchaseListener.onPurchaseFailed]
     * is invoked and no popup is shown.
     *
     * This is the recommended, batteries-included purchase path. For a custom UI, use [makePurchase].
     */
    @JvmStatic
    fun showPurchasePopup(
        activity: Activity,
        itemId: String,
        listener: PurchaseListener,
    ) {
        val rt = runtime
        if (rt == null) {
            listener.onPurchaseFailed(PurchaseSdkError.NotInitialized)
            return
        }
        mainScope.launch {
            try {
                val item = rt.purchaseManager.loadItem(itemId)
                rt.popupController.showPopup(activity, item, rt.userId, listener)
            } catch (e: PurchaseException) {
                listener.onPurchaseFailed(e.error)
            } catch (t: Throwable) {
                listener.onPurchaseFailed(ErrorMapper.mapThrowableToSdkError(t))
            }
        }
    }

    /**
     * Runs the purchase flow directly, without showing a popup — for apps that build their own UI.
     *
     * @return the resulting [PurchaseResult] on success.
     * @throws PurchaseException on any failure (e.g. `SDK_NOT_INITIALIZED`, `ITEM_NOT_FOUND`,
     *   `NETWORK_ERROR`), with the structured error in `e.error`.
     */
    suspend fun makePurchase(itemId: String, paymentMethodId: String? = null): PurchaseResult {
        val rt = requireRuntime()
        return rt.purchaseManager.makePurchase(itemId, paymentMethodId)
    }

    /**
     * Restores the current user's prior purchases and refreshes the local entitlement cache.
     *
     * @param itemId when non-null, only that single item is restored/returned (per-item return);
     *   when null (the default), every owned item is restored.
     * @throws PurchaseException `SDK_NOT_INITIALIZED` if not initialized.
     */
    suspend fun restorePurchases(itemId: String? = null): List<PurchaseResult> {
        val rt = requireRuntime()
        return rt.purchaseManager.restorePurchases(itemId)
    }

    /**
     * Returns whether the current user has active access to [itemId]. Checks the local cache first,
     * then refreshes from the API if needed.
     *
     * @throws PurchaseException `SDK_NOT_INITIALIZED` if not initialized.
     */
    suspend fun hasEntitlement(itemId: String): Boolean {
        val rt = requireRuntime()
        val result = rt.entitlementManager.hasEntitlement(itemId)
        rt.analytics.trackHasEntitlementChecked(itemId, result)
        return result
    }

    /**
     * Returns all active entitlements for the current user.
     *
     * @throws PurchaseException `SDK_NOT_INITIALIZED` if not initialized.
     */
    suspend fun listEntitlements(): List<UserEntitlement> {
        val rt = requireRuntime()
        rt.analytics.trackEntitlementsListed()
        return rt.entitlementManager.listEntitlements()
    }

    /**
     * Clears the current user's local data (user id + entitlement cache), stops background work, and
     * resets the SDK to the uninitialized state. The cached item catalog is preserved. Safe to call
     * even if not initialized.
     */
    @JvmStatic
    fun logout() {
        runtime?.let { rt ->
            rt.entitlementManager.clearEntitlements()
            rt.localStorage.clearUserData()
            rt.shutdown()
        }
        runtime = null
    }

    /**
     * Sends a custom analytics event through the SDK. No-op if the SDK is not initialized or if
     * analytics are disabled in [PurchaseSdkConfig].
     */
    @JvmStatic
    @JvmOverloads
    fun trackEvent(eventName: String, properties: Map<String, Any> = emptyMap()) {
        runtime?.analytics?.trackCustomEvent(eventName, properties)
    }

    /** Returns the active runtime or throws a structured "not initialized" error. */
    private fun requireRuntime(): SdkRuntime =
        runtime ?: throw PurchaseException(PurchaseSdkError.NotInitialized)
}
