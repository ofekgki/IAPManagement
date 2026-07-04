package com.example.iapsdk.models

/**
 * Outcome of a purchase attempt, returned from the purchase flow and delivered to
 * [com.example.iapsdk.listener.PurchaseListener.onPurchaseSuccess].
 *
 * @property purchaseId  Server-assigned identifier for this purchase transaction.
 * @property itemId      The [PurchaseItem.id] that was purchased.
 * @property itemName    Human-readable product name, when the server provides it (e.g. on restore).
 * @property userId      The user the purchase is attributed to.
 * @property status      Final (or current) state of the purchase.
 * @property purchasedAt Epoch milliseconds when the purchase was recorded.
 * @property message     Optional human-readable note (e.g. a failure reason).
 * @property entitlementStatus Current access for this item on restore ("ACTIVE" / "REVOKED" /
 *   "EXPIRED"), or null when the item grants no long-lived entitlement. Restore never changes it.
 */
data class PurchaseResult(
    val purchaseId: String,
    val itemId: String,
    val userId: String,
    val status: PurchaseStatus,
    val purchasedAt: Long,
    val message: String? = null,
    val itemName: String? = null,
    val entitlementStatus: String? = null,
)
