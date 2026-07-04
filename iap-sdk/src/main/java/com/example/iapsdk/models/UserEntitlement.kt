package com.example.iapsdk.models

/**
 * Represents access the current user has to a given item — what they own or are subscribed to.
 *
 * @property itemId    The [PurchaseItem.id] this entitlement grants access to.
 * @property userId    The user who holds the entitlement.
 * @property type      Commercial nature of the underlying item.
 * @property isActive  Whether the entitlement currently grants access (false once expired/revoked).
 * @property grantedAt Epoch milliseconds when access was granted.
 * @property expiresAt Epoch milliseconds when access ends, or `null` for non-expiring entitlements.
 */
data class UserEntitlement(
    val itemId: String,
    val userId: String,
    val type: PurchaseItemType,
    val isActive: Boolean,
    val grantedAt: Long,
    val expiresAt: Long? = null,
)
