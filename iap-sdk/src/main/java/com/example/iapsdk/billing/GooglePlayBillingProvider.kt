package com.example.iapsdk.billing

import android.content.Context
import com.example.iapsdk.config.PurchaseSdkConfig
import com.example.iapsdk.models.PurchaseException
import com.example.iapsdk.models.PurchaseItem
import com.example.iapsdk.models.PurchaseResult
import com.example.iapsdk.models.PurchaseSdkError

/**
 * Scaffold for **real Google Play Billing**. The structure is in place so wiring up real billing
 * later is a localized change, but no real billing is implemented yet — every entry point fails
 * gracefully with [PurchaseSdkError.GooglePlayNotConfigured] rather than pretending to work.
 *
 * Implementation checklist (each is a real step required to enable this mode):
 *
 * - TODO(google-play): Add the Google Play Billing dependency to build.gradle when enabling real
 *   billing, e.g. `implementation("com.android.billingclient:billing-ktx:<version>")`.
 * - TODO(google-play): Initialize a `BillingClient` here (with this [context]) and a
 *   `PurchasesUpdatedListener`.
 * - TODO(google-play): Connect to the Google Play Billing service
 *   (`startConnection` / `BillingClientStateListener`) and retry on disconnect.
 * - TODO(google-play): Load `ProductDetails` from your Google Play Console product IDs
 *   (`queryProductDetailsAsync`). Do NOT hardcode product IDs/prices — read them from Play.
 * - TODO(google-play): Launch `BillingClient.launchBillingFlow(activity, params)` from an Activity
 *   context. (This provider currently has no Activity; pass one through when wiring real billing.)
 * - TODO(google-play): Handle the `PurchasesUpdatedListener` result and resume the suspended call.
 * - TODO(google-play): Verify the purchase token with your backend BEFORE granting any entitlement
 *   (otherwise return [PurchaseSdkError.VerificationRequired]).
 * - TODO(google-play): Acknowledge non-consumable / subscription purchases (`acknowledgePurchase`).
 * - TODO(google-play): Consume consumable purchases (`consumeAsync`) if/when the SDK supports them.
 * - TODO(google-play): Map Google `BillingResponseCode` values to [PurchaseSdkError] (see ErrorMapper).
 * - TODO(google-play): Use `setObfuscatedAccountId(...)` with a HASHED user/account id — never raw
 *   email or raw user data.
 * - TODO(backend): Provide the real API key / auth needed for server-side token verification.
 */
internal class GooglePlayBillingProvider(
    @Suppress("unused") private val context: Context,
    @Suppress("unused") private val config: PurchaseSdkConfig,
) : BillingProvider {

    // TODO(google-play): private lateinit var billingClient: BillingClient
    // TODO(google-play): ensure the client is connected (and ProductDetails loaded) before each call.

    override suspend fun getItem(itemId: String): PurchaseItem? {
        // TODO(google-play): return a PurchaseItem mapped from queried ProductDetails for itemId.
        throw PurchaseException(PurchaseSdkError.GooglePlayNotConfigured)
    }

    override suspend fun getItems(): List<PurchaseItem> {
        // TODO(google-play): return PurchaseItems mapped from queryProductDetailsAsync results.
        throw PurchaseException(PurchaseSdkError.GooglePlayNotConfigured)
    }

    override suspend fun makePurchase(itemId: String, userId: String?, paymentMethod: String?): PurchaseResult {
        // TODO(google-play): launch the real billing flow, await PurchasesUpdatedListener,
        //  verify the token server-side, acknowledge/consume, then map to a PurchaseResult.
        throw PurchaseException(PurchaseSdkError.GooglePlayNotConfigured)
    }

    override suspend fun restorePurchases(userId: String?, itemId: String?): List<PurchaseResult> {
        // TODO(google-play): queryPurchasesAsync(...) for INAPP and SUBS, verify, and map to results.
        throw PurchaseException(PurchaseSdkError.GooglePlayNotConfigured)
    }
}
