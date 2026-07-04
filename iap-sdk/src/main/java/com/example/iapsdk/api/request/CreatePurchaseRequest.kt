package com.example.iapsdk.api.request

/**
 * Body for `ApiClient.createPurchase`.
 *
 * @property itemId          The item being purchased.
 * @property userId          The buyer.
 * @property paymentMethodId Selected payment method, or `null` to let the backend pick a default.
 * @property idempotencyKey  Stable key derived from (itemId, userId) so retried/double-tapped
 *                           requests collapse into a single purchase server-side.
 * @property requestId       Unique id for this specific request attempt (for tracing/logging).
 */
internal data class CreatePurchaseRequest(
    val itemId: String,
    val userId: String,
    val paymentMethodId: String?,
    val idempotencyKey: String,
    val requestId: String,
)
