package com.example.iapsdk.billing

import com.example.iapsdk.api.ApiClient
import com.example.iapsdk.api.request.CreatePurchaseRequest
import com.example.iapsdk.api.toPurchaseItem
import com.example.iapsdk.api.toPurchaseResult
import com.example.iapsdk.config.PurchaseSdkConfig
import com.example.iapsdk.entitlement.EntitlementManager
import com.example.iapsdk.models.PurchaseItem
import com.example.iapsdk.models.PurchaseResult
import com.example.iapsdk.models.UserEntitlement
import com.example.iapsdk.storage.LocalStorage
import com.example.iapsdk.util.ANONYMOUS_USER_ID
import com.example.iapsdk.util.generateIdempotencyKey
import com.example.iapsdk.util.generateRequestId

/**
 * Fully working, **simulated** billing. Requires no Google Play setup, opens no Google UI, and makes
 * no real payment — perfect for the demo app, tests, and this educational project.
 *
 * It reuses the mock [ApiClient] (sample catalog + in-memory purchase store) for the "transaction"
 * and grants entitlements locally via [EntitlementManager] / [LocalStorage], so `hasEntitlement`,
 * `listEntitlements`, and `restorePurchases` all behave end-to-end.
 */
internal class MockBillingProvider(
    private val apiClient: ApiClient,
    private val entitlementManager: EntitlementManager,
    private val localStorage: LocalStorage,
    private val config: PurchaseSdkConfig,
) : BillingProvider {

    /** Item lookup, cache-first when caching is enabled. Returns null if the catalog has no match. */
    override suspend fun getItem(itemId: String): PurchaseItem? {
        if (config.enableLocalCache) localStorage.getItem(itemId)?.let { return it }
        val item = runCatching { apiClient.fetchItem(itemId).toPurchaseItem() }.getOrNull()
        if (item != null && config.enableLocalCache) localStorage.saveItem(item)
        return item
    }

    /** Loads the full catalog from the backend, warming the local item cache when enabled. */
    override suspend fun getItems(): List<PurchaseItem> {
        val items = apiClient.fetchItems().map { it.toPurchaseItem() }
        if (config.enableLocalCache) items.forEach { localStorage.saveItem(it) }
        return items
    }

    /**
     * Simulates a successful purchase: create → confirm against the mock API, then grant the
     * entitlement locally. Returns a fake [PurchaseResult]. No real billing occurs.
     */
    override suspend fun makePurchase(itemId: String, userId: String?, paymentMethod: String?): PurchaseResult {
        val uid = userId ?: ANONYMOUS_USER_ID
        val item = apiClient.fetchItem(itemId).toPurchaseItem() // throws ItemNotFound if missing

        val request = CreatePurchaseRequest(
            itemId = item.id,
            userId = uid,
            paymentMethodId = paymentMethod,
            idempotencyKey = generateIdempotencyKey(item.id, uid),
            requestId = generateRequestId(),
        )
        val created = apiClient.createPurchase(request)
        val confirmed = apiClient.confirmPurchase(created.purchaseId)
        val result = confirmed.toPurchaseResult()

        // Grant the entitlement locally so the app sees access immediately (demo behavior).
        entitlementManager.addOrUpdateEntitlement(
            UserEntitlement(
                itemId = item.id,
                userId = uid,
                type = item.type,
                isActive = true,
                grantedAt = result.purchasedAt,
                expiresAt = null,
            ),
        )
        return result
    }

    /** Returns prior successful purchases and refreshes the local entitlement cache from them. */
    override suspend fun restorePurchases(userId: String?, itemId: String?): List<PurchaseResult> {
        val uid = userId ?: ANONYMOUS_USER_ID
        val results = apiClient.restorePurchases(uid, itemId).map { it.toPurchaseResult() }
        entitlementManager.refreshEntitlements()
        return results
    }
}
