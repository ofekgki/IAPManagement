package com.example.iapmanagement.demo

import com.example.iapsdk.models.PurchaseItem
import com.example.iapsdk.models.PurchaseResult
import com.example.iapsdk.models.UserEntitlement

/**
 * Immutable snapshot of everything the demo UI renders. The [DemoViewModel] is the single owner; the
 * screens are pure functions of this state (no business logic in the UI).
 */
data class DemoUiState(
    // Store
    val itemsLoading: Boolean = false,
    val items: List<PurchaseItem> = emptyList(),
    val itemsError: String? = null,
    /** Item ids the demo user currently has an active entitlement for (drives "Owned" badges). */
    val ownedItemIds: Set<String> = emptySet(),

    // Entitlements
    val entitlementsLoading: Boolean = false,
    val entitlements: List<UserEntitlement> = emptyList(),
    val entitlementsError: String? = null,

    // Restore
    val restore: RestoreState = RestoreState.Idle,
    /** Item id currently being returned (per-item return), so only that row shows a spinner. */
    val restoringItemId: String? = null,

    // Premium feature gate
    val premiumLoading: Boolean = false,
    val premiumUnlocked: Boolean = false,

    /** One-shot user-facing message (shown as a banner, then cleared). */
    val message: String? = null,
)

/** State machine for the "Restore purchases" flow: idle → loading → success/error. */
sealed interface RestoreState {
    data object Idle : RestoreState
    data object Loading : RestoreState
    data class Success(val results: List<PurchaseResult>) : RestoreState
    data class Error(val message: String) : RestoreState
}
