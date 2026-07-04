package com.example.iapsdk.util

import java.util.UUID

/**
 * Fallback user id used when the host app initializes the SDK without one (allowed in MOCK mode).
 * Demo/educational value only — a real deployment should always pass a stable user id.
 */
internal const val ANONYMOUS_USER_ID = "anonymous_demo_user"

/**
 * Small id helpers used by the purchase flow.
 *
 * - [generateRequestId] is unique per attempt — good for tracing a single network call.
 * - [generateIdempotencyKey] is unique per purchase *attempt*. It must NOT be stable across attempts:
 *   a stable (item, user) key would make the backend replay the first purchase's confirm response on
 *   every later purchase of the same item (e.g. buying again after a restore/refund), leaving the new
 *   purchase stuck at CREATED and never re-granting the entitlement. The key is generated once per
 *   [com.example.iapsdk.api.ApiClient.createPurchase] call and reused for that purchase's confirm
 *   retries (it's stored in the in-flight `pending` map), which is the scope idempotency should cover.
 */

internal fun generateRequestId(): String = "req_${UUID.randomUUID()}"

internal fun generateIdempotencyKey(itemId: String, userId: String): String =
    "idem_${itemId}_${userId}_${UUID.randomUUID()}"
