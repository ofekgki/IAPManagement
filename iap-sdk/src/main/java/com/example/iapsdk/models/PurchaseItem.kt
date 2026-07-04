package com.example.iapsdk.models

/**
 * A purchasable product as exposed to the client app.
 *
 * This is the public, transport-agnostic model. Internally the SDK maps its network
 * `PurchaseItemDto` into this type so the client never depends on wire formats.
 *
 * @property id          Stable product identifier (e.g. "remove_ads").
 * @property name        Display name shown to the user.
 * @property description Short marketing/explanatory text.
 * @property price       Numeric price in [currency]. Formatting for display is the caller's choice.
 * @property currency    ISO-4217 currency code (e.g. "USD").
 * @property type        Commercial nature of the item (see [PurchaseItemType]).
 */
data class PurchaseItem(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val currency: String,
    val type: PurchaseItemType,
)
