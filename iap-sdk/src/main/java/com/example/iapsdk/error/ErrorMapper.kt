package com.example.iapsdk.error

import com.example.iapsdk.models.PurchaseException
import com.example.iapsdk.models.PurchaseSdkError
import java.io.IOException

/**
 * Single place that turns anything that can go wrong into a stable [PurchaseSdkError].
 *
 * This is where mock failures and (later) Google Play `BillingResponseCode`s get normalized into the
 * SDK's closed error set, so the rest of the SDK and the host app deal only with [PurchaseSdkError].
 */
internal object ErrorMapper {

    /**
     * Normalizes any throwable into a [PurchaseSdkError]. A [PurchaseException] already carries a
     * structured error, so it is unwrapped; IO failures map to [PurchaseSdkError.BillingUnavailable];
     * anything else becomes [PurchaseSdkError.Unknown].
     */
    fun mapThrowableToSdkError(throwable: Throwable): PurchaseSdkError = when (throwable) {
        is PurchaseException -> throwable.error
        is IOException -> PurchaseSdkError.BillingUnavailable
        else -> PurchaseSdkError.Unknown(throwable.message ?: "An unexpected error occurred.")
    }

    // TODO(google-play): Map Google BillingClient.BillingResponseCode values to PurchaseSdkError here,
    //  e.g. USER_CANCELED -> PurchaseCancelled, SERVICE_UNAVAILABLE/SERVICE_DISCONNECTED ->
    //  BillingUnavailable, ITEM_UNAVAILABLE -> ItemNotFound, ERROR -> PurchaseFailed.
    // fun mapBillingResponseCode(@BillingResponseCode code: Int): PurchaseSdkError { ... }

    fun notInitialized(): PurchaseSdkError = PurchaseSdkError.NotInitialized

    fun itemNotFound(): PurchaseSdkError = PurchaseSdkError.ItemNotFound

    fun googlePlayNotConfigured(): PurchaseSdkError = PurchaseSdkError.GooglePlayNotConfigured
}
