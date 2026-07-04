package com.example.iapsdk.entitlement

import com.example.iapsdk.api.ApiClient
import com.example.iapsdk.api.toUserEntitlement
import com.example.iapsdk.config.PurchaseSdkConfig
import com.example.iapsdk.models.UserEntitlement
import com.example.iapsdk.storage.LocalStorage
import com.example.iapsdk.util.ANONYMOUS_USER_ID

/**
 * Owns "what does the user have access to". Keeps entitlement concerns out of [PurchaseManager];
 * the public `hasEntitlement` / `listEntitlements` delegate here.
 *
 * Read strategy: serve from the local cache when available (and caching is enabled), otherwise
 * refresh from the API. Writes go to both an in-memory list and [LocalStorage] so access survives
 * process restarts.
 */
internal class EntitlementManager(
    private val apiClient: ApiClient,
    private val localStorage: LocalStorage,
    private val config: PurchaseSdkConfig,
    private val userId: String?,
) {

    /** In-memory mirror of the cache, seeded from storage so reads are instant after construction. */
    private val cache: MutableList<UserEntitlement> =
        if (config.enableLocalCache) localStorage.getEntitlements().toMutableList() else mutableListOf()

    /** True if the user currently holds an active entitlement for [itemId]. */
    suspend fun hasEntitlement(itemId: String): Boolean =
        currentEntitlements().any { it.itemId == itemId && it.isActive }

    /** Returns the user's active entitlements. */
    suspend fun listEntitlements(): List<UserEntitlement> =
        currentEntitlements().filter { it.isActive }

    /**
     * Authoritative entitlement read: the backend is the source of truth, so this **refreshes from the
     * API** and only falls back to the local cache when the network is unavailable. This is what makes
     * the app reflect server-side changes — a portal **revoke**, a subscription **expiry**, or a
     * **restore** — instead of showing stale "owned" state from a previous session.
     */
    private suspend fun currentEntitlements(): List<UserEntitlement> =
        try {
            refreshEntitlements()
        } catch (t: Throwable) {
            getCachedEntitlements() // offline / backend unreachable: best-effort from cache
        }

    /** Forces a refresh from the API and replaces the cache with the result. */
    suspend fun refreshEntitlements(): List<UserEntitlement> {
        val fresh = apiClient.fetchEntitlements(userId ?: ANONYMOUS_USER_ID)
            .map { it.toUserEntitlement() }
        cacheEntitlements(fresh)
        return fresh
    }

    /** Replaces the whole cache (in-memory + persistent). */
    fun cacheEntitlements(entitlements: List<UserEntitlement>) {
        cache.clear()
        cache.addAll(entitlements)
        persist()
    }

    fun getCachedEntitlements(): List<UserEntitlement> =
        if (config.enableLocalCache) cache.toList() else emptyList()

    /** Upserts one entitlement (matched by itemId) — used after a successful purchase. */
    fun addOrUpdateEntitlement(entitlement: UserEntitlement) {
        cache.removeAll { it.itemId == entitlement.itemId }
        cache.add(entitlement)
        persist()
    }

    fun clearEntitlements() {
        cache.clear()
        localStorage.saveEntitlements(emptyList())
    }

    private fun persist() {
        if (config.enableLocalCache) localStorage.saveEntitlements(cache.toList())
    }
}
