package com.example.iapsdk.listener

import com.example.iapsdk.models.PurchaseResult
import com.example.iapsdk.models.PurchaseSdkError

/**
 * Receives the outcome of a popup-driven purchase started via
 * [com.example.iapsdk.PurchaseSdk.showPurchasePopup].
 *
 * Exactly one method is invoked per popup session, always on the main thread.
 */
interface PurchaseListener {

    /** The purchase completed successfully. [result] describes the transaction. */
    fun onPurchaseSuccess(result: PurchaseResult)

    /** The user dismissed the popup (Cancel button, back press, or tap outside). */
    fun onPurchaseCancelled()

    /** The purchase failed. [error] is the normalized, client-facing reason. */
    fun onPurchaseFailed(error: PurchaseSdkError)
}
