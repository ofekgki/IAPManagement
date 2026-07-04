package com.example.iapsdk.core

import android.content.Context
import com.example.iapsdk.analytics.AnalyticsTracker
import com.example.iapsdk.api.ApiClient
import com.example.iapsdk.billing.BillingProvider
import com.example.iapsdk.billing.GooglePlayBillingProvider
import com.example.iapsdk.billing.MockBillingProvider
import com.example.iapsdk.config.BillingMode
import com.example.iapsdk.config.PurchaseSdkConfig
import com.example.iapsdk.entitlement.EntitlementManager
import com.example.iapsdk.purchase.PurchaseManager
import com.example.iapsdk.storage.LocalStorage
import com.example.iapsdk.ui.PurchasePopupController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Immutable container for one initialized SDK session: credentials, config, the chosen
 * [BillingMode] + its [BillingProvider], all internal managers (wired in dependency order), and the
 * coroutine scope that outlives any single call.
 *
 * [com.example.iapsdk.PurchaseSdk] holds a single nullable [SdkRuntime]; it is created in [create] on
 * `init` and dropped on `logout`, which makes "initialized" a simple null check and guarantees an
 * Activity is never retained (only [appContext] is kept).
 */
internal class SdkRuntime private constructor(
    val appContext: Context,
    val apiKey: String,
    val userId: String?,
    val billingMode: BillingMode,
    val config: PurchaseSdkConfig,
    val localStorage: LocalStorage,
    val apiClient: ApiClient,
    val analytics: AnalyticsTracker,
    val entitlementManager: EntitlementManager,
    val purchaseManager: PurchaseManager,
    val popupController: PurchasePopupController,
    private val scope: CoroutineScope,
) {

    /** Cancels background work (e.g. in-flight analytics) for this session. */
    fun shutdown() = scope.cancel()

    companion object {
        /** Builds a fully wired runtime from validated inputs, selecting the provider by [billingMode]. */
        fun create(
            context: Context,
            apiKey: String,
            userId: String?,
            billingMode: BillingMode,
            config: PurchaseSdkConfig,
        ): SdkRuntime {
            val appContext = context.applicationContext
            // Background scope for fire-and-forget work; survives individual suspend calls.
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

            val localStorage = LocalStorage(appContext)
            val apiClient = ApiClient(apiKey, config)
            val analytics = AnalyticsTracker(apiClient, config, billingMode, userId, scope)
            val entitlementManager = EntitlementManager(apiClient, localStorage, config, userId)

            // Build both providers; PurchaseManager selects the one matching billingMode. (Provider
            // constructors are side-effect free, so building the unused one is harmless.)
            val mockProvider: BillingProvider =
                MockBillingProvider(apiClient, entitlementManager, localStorage, config)
            val googlePlayProvider: BillingProvider =
                GooglePlayBillingProvider(appContext, config)

            val purchaseManager = PurchaseManager(
                billingMode = billingMode,
                mockProvider = mockProvider,
                googlePlayProvider = googlePlayProvider,
                analytics = analytics,
                userId = userId,
            )
            val popupController = PurchasePopupController(purchaseManager, analytics)

            return SdkRuntime(
                appContext = appContext,
                apiKey = apiKey,
                userId = userId,
                billingMode = billingMode,
                config = config,
                localStorage = localStorage,
                apiClient = apiClient,
                analytics = analytics,
                entitlementManager = entitlementManager,
                purchaseManager = purchaseManager,
                popupController = popupController,
                scope = scope,
            )
        }
    }
}
