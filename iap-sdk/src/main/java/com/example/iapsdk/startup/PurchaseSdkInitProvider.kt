package com.example.iapsdk.startup

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.example.iapsdk.PurchaseSdk
import com.example.iapsdk.config.BillingMode
import com.example.iapsdk.config.PurchaseSdkConfig

/**
 * Zero-config auto-initialization — the same trick Firebase uses (`FirebaseInitProvider`).
 *
 * Android instantiates every declared [ContentProvider] and calls [onCreate] **before**
 * `Application.onCreate`, so this runs automatically at process start with no host-app code. It reads
 * `<meta-data>` from the merged manifest and, if an API key is present, calls [PurchaseSdk.init].
 *
 * If no meta-data is configured it is a **no-op**, and the host app initializes manually as before —
 * so this is purely additive and safe.
 *
 * ### Host-app configuration (AndroidManifest.xml)
 * ```xml
 * <application>
 *   <meta-data android:name="com.example.iapsdk.API_KEY"      android:value="demo_api_key_123" />
 *   <meta-data android:name="com.example.iapsdk.BASE_URL"     android:value="http://10.0.2.2:8080/api/v1/sdk" />
 *   <meta-data android:name="com.example.iapsdk.BILLING_MODE" android:value="MOCK" />
 * </application>
 * ```
 * The user id is usually unknown at process start, so it is optional here; call
 * `PurchaseSdk.init(..., userId = currentUser.id)` once you know the user (it replaces the session).
 */
class PurchaseSdkInitProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        val ctx = context ?: return false
        val meta = readMetaData(ctx) ?: return true
        val apiKey = meta.string(KEY_API_KEY) ?: return true // no config -> manual init path

        val billingMode = runCatching {
            BillingMode.valueOf((meta.string(KEY_BILLING_MODE) ?: "MOCK").uppercase())
        }.getOrDefault(BillingMode.MOCK)

        PurchaseSdk.init(
            context = ctx.applicationContext,
            apiKey = apiKey,
            billingMode = billingMode,
            userId = meta.string(KEY_USER_ID),
            config = PurchaseSdkConfig(baseUrl = meta.string(KEY_BASE_URL)),
        )
        Log.i(TAG, "Auto-initialized PurchaseSdk from manifest meta-data (billingMode=$billingMode).")
        return true
    }

    private fun readMetaData(ctx: Context): Bundle? = runCatching {
        @Suppress("DEPRECATION")
        ctx.packageManager.getApplicationInfo(ctx.packageName, PackageManager.GET_META_DATA).metaData
    }.getOrNull()

    /** Reads a manifest meta-data value as a trimmed, non-empty string (or null). */
    private fun Bundle.string(key: String): String? =
        get(key)?.toString()?.trim()?.ifEmpty { null }

    // This provider exists only for its startup timing; the data-provider surface is unused.
    override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?): Int = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?): Int = 0

    private companion object {
        const val TAG = "PurchaseSdkInit"
        const val KEY_API_KEY = "com.example.iapsdk.API_KEY"
        const val KEY_BASE_URL = "com.example.iapsdk.BASE_URL"
        const val KEY_BILLING_MODE = "com.example.iapsdk.BILLING_MODE"
        const val KEY_USER_ID = "com.example.iapsdk.USER_ID"
    }
}
