package com.example.iapmanagement.demo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.iapsdk.PurchaseSdk
import com.example.iapsdk.models.PurchaseException
import com.example.iapsdk.models.PurchaseResult
import com.example.iapsdk.models.PurchaseSdkError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The demo app's single source of truth and the only place that talks to [PurchaseSdk]. It keeps all
 * business logic out of the Composable screens (which just read [uiState] and call these methods).
 *
 * Not an AndroidX `ViewModel` (the demo deliberately adds no extra dependency) but plays the same
 * role: it owns a coroutine [scope] and exposes Compose state. The Activity creates one and calls
 * [dispose] in `onDestroy`.
 *
 * Every method demonstrates a specific public SDK function — see the comments on each.
 */
class DemoViewModel {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    var uiState by mutableStateOf(DemoUiState())
        private set

    // --- Store ------------------------------------------------------------------------------

    /** Loads the catalog from the backend via [PurchaseSdk.getItems] and marks owned items. */
    fun loadStore() {
        uiState = uiState.copy(itemsLoading = true, itemsError = null)
        scope.launch {
            try {
                val items = PurchaseSdk.getItems()
                // Demonstrates a custom analytics event per browsed item (ITEM_VIEWED).
                items.forEach { PurchaseSdk.trackEvent("item_viewed", mapOf("item_id" to it.id)) }
                val owned = activeOwnedItemIds()
                uiState = uiState.copy(
                    itemsLoading = false,
                    items = items,
                    ownedItemIds = owned,
                )
            } catch (e: PurchaseException) {
                uiState = uiState.copy(itemsLoading = false, itemsError = e.error.message)
            }
        }
    }

    // --- Entitlements -----------------------------------------------------------------------

    /** Loads the user's active entitlements via [PurchaseSdk.listEntitlements]. */
    fun loadEntitlements() {
        uiState = uiState.copy(entitlementsLoading = true, entitlementsError = null)
        scope.launch {
            try {
                val entitlements = PurchaseSdk.listEntitlements()
                uiState = uiState.copy(
                    entitlementsLoading = false,
                    entitlements = entitlements,
                    ownedItemIds = entitlements.filter { it.isActive }.map { it.itemId }.toSet(),
                )
            } catch (e: PurchaseException) {
                uiState = uiState.copy(entitlementsLoading = false, entitlementsError = e.error.message)
            }
        }
    }

    // --- Restore ----------------------------------------------------------------------------

    /** Returns ALL of the user's owned purchases via [PurchaseSdk.restorePurchases] (restore-all). */
    fun restorePurchases() = runRestore(itemId = null)

    /** Returns a single owned item (per-item return) via [PurchaseSdk.restorePurchases]. */
    fun returnItem(itemId: String) = runRestore(itemId = itemId)

    /**
     * Shared restore flow (emits restore_* analytics). [itemId] null returns everything owned; a
     * non-null id returns just that item. Refreshes the catalog afterwards since returning releases
     * ownership (the item becomes buyable again).
     */
    private fun runRestore(itemId: String?) {
        uiState = uiState.copy(restore = RestoreState.Loading, restoringItemId = itemId)
        scope.launch {
            try {
                val results = PurchaseSdk.restorePurchases(itemId)
                uiState = uiState.copy(restore = RestoreState.Success(results), restoringItemId = null)
                // Restore returns items (releases ownership), so refresh what the user owns.
                loadStore()
            } catch (e: PurchaseException) {
                uiState = uiState.copy(restore = RestoreState.Error(e.error.message), restoringItemId = null)
            }
        }
    }

    // --- Premium feature gate ---------------------------------------------------------------

    /** Checks access to the premium item via [PurchaseSdk.hasEntitlement]. */
    fun checkPremiumAccess(itemId: String = DemoConfig.PREMIUM_FEATURE_ITEM_ID) {
        uiState = uiState.copy(premiumLoading = true)
        scope.launch {
            try {
                val unlocked = PurchaseSdk.hasEntitlement(itemId)
                // Demonstrates a custom analytics event (ENTITLEMENT_CHECKED).
                PurchaseSdk.trackEvent(
                    "entitlement_checked",
                    mapOf("item_id" to itemId, "result" to unlocked),
                )
                uiState = uiState.copy(premiumLoading = false, premiumUnlocked = unlocked)
            } catch (e: PurchaseException) {
                uiState = uiState.copy(premiumLoading = false, message = e.error.message)
            }
        }
    }

    // --- Purchase outcome callbacks (driven by the SDK popup in MainActivity) ----------------

    fun onPurchaseSuccess(result: PurchaseResult) {
        uiState = uiState.copy(message = "Purchase complete: ${result.itemId}")
        // Refresh everything that "owning" affects.
        loadEntitlements()
        loadStore()
        checkPremiumAccess()
    }

    fun onPurchaseCancelled() {
        uiState = uiState.copy(message = "Purchase cancelled.")
    }

    fun onPurchaseFailed(error: PurchaseSdkError) {
        uiState = uiState.copy(message = "Purchase failed: ${error.message}")
    }

    fun clearMessage() {
        uiState = uiState.copy(message = null)
    }

    fun dispose() {
        scope.cancel()
    }

    /** Pulls the current active entitlements without flipping the entitlements-screen loading flag. */
    private suspend fun activeOwnedItemIds(): Set<String> =
        try {
            PurchaseSdk.listEntitlements().filter { it.isActive }.map { it.itemId }.toSet()
        } catch (e: PurchaseException) {
            emptySet()
        }
}
