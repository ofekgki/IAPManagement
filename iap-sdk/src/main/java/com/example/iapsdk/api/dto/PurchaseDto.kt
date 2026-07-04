package com.example.iapsdk.api.dto

/**
 * Wire representation of a purchase transaction. Map with `PurchaseDto.toPurchaseResult()`.
 */
internal data class PurchaseDto(
    val purchaseId: String,
    val itemId: String,
    val userId: String,
    /** Server sends the status as a string; the mapper parses it into `PurchaseStatus`. */
    val status: String,
    val purchasedAt: Long,
    val message: String? = null,
    /** Human-readable product name, when the server provides it (e.g. on restore). */
    val itemName: String? = null,
    /** Current entitlement access on restore ("ACTIVE" / "REVOKED" / "EXPIRED"), or null. */
    val entitlementStatus: String? = null,
)
