package com.example.iapsdk.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.iapsdk.config.PurchaseSdkConfig
import com.example.iapsdk.models.PurchaseItem
import com.example.iapsdk.models.UserEntitlement
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * The SDK's local persistence: credentials, config, and the item/entitlement caches.
 *
 * Backed by a private [SharedPreferences] file with values serialized as JSON via [Gson]. This is
 * deliberately the simplest durable option for v1; it can later be swapped for DataStore or Room
 * behind this same internal surface without touching callers.
 *
 * All access goes through this class — no other part of the SDK touches SharedPreferences directly.
 */
internal class LocalStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    // --- Credentials -------------------------------------------------------------------------

    fun saveApiKey(apiKey: String) = prefs.edit().putString(KEY_API_KEY, apiKey).apply()

    fun getApiKey(): String? = prefs.getString(KEY_API_KEY, null)

    fun saveUserId(userId: String) = prefs.edit().putString(KEY_USER_ID, userId).apply()

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    // --- Config ------------------------------------------------------------------------------

    fun saveConfig(config: PurchaseSdkConfig) =
        prefs.edit().putString(KEY_CONFIG, gson.toJson(config)).apply()

    fun getConfig(): PurchaseSdkConfig? =
        prefs.getString(KEY_CONFIG, null)?.let { gson.fromJson(it, PurchaseSdkConfig::class.java) }

    // --- Item cache --------------------------------------------------------------------------

    fun saveItems(items: List<PurchaseItem>) =
        prefs.edit().putString(KEY_ITEMS, gson.toJson(items)).apply()

    fun getItems(): List<PurchaseItem> {
        val json = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        val type = object : TypeToken<List<PurchaseItem>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    /** Upserts a single item into the cached list (matched by id). */
    fun saveItem(item: PurchaseItem) {
        val updated = getItems().filterNot { it.id == item.id } + item
        saveItems(updated)
    }

    fun getItem(itemId: String): PurchaseItem? = getItems().firstOrNull { it.id == itemId }

    // --- Entitlement cache -------------------------------------------------------------------

    fun saveEntitlements(entitlements: List<UserEntitlement>) =
        prefs.edit().putString(KEY_ENTITLEMENTS, gson.toJson(entitlements)).apply()

    fun getEntitlements(): List<UserEntitlement> {
        val json = prefs.getString(KEY_ENTITLEMENTS, null) ?: return emptyList()
        val type = object : TypeToken<List<UserEntitlement>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    // --- Clearing ----------------------------------------------------------------------------

    /** Clears per-user data (user id + entitlements) but keeps the item catalog cache. */
    fun clearUserData() {
        prefs.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_ENTITLEMENTS)
            .apply()
    }

    /** Wipes everything this SDK stored. */
    fun clearAll() = prefs.edit().clear().apply()

    private companion object {
        const val PREFS_NAME = "com.example.iapsdk.storage"
        const val KEY_API_KEY = "api_key"
        const val KEY_USER_ID = "user_id"
        const val KEY_CONFIG = "config"
        const val KEY_ITEMS = "items"
        const val KEY_ENTITLEMENTS = "entitlements"
    }
}
