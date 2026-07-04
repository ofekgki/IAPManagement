package com.example.iapsdk.api.dto

/**
 * Wire representation of a purchasable item, as the backend would return it.
 *
 * Kept separate from the public `PurchaseItem` so transport concerns (string enums, nullable
 * fields, extra server-only metadata) never leak into the client-facing API. Map with
 * `PurchaseItemDto.toPurchaseItem()`.
 */
internal data class PurchaseItemDto(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val currency: String,
    /** Server sends the type as a string; the mapper parses it into `PurchaseItemType`. */
    val type: String,
)
