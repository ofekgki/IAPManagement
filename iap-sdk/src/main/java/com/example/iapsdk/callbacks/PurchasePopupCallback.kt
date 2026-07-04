package com.example.iapsdk.callbacks

import com.example.iapsdk.model.PurchasePopupData

/**
 * Listener for the outcome of a purchase pop-up.
 *
 * Kotlin callers will typically prefer the lambda-based overloads on
 * [com.example.iapsdk.ui.PurchaseDialog.show]; this interface exists so the SDK is equally
 * pleasant to use from Java and so the callback contract has a single, documented home.
 *
 * Exactly one of [onConfirm] / [onCancel] is invoked per pop-up, on the main thread.
 *
 * Note: for now [onConfirm] only hands back the data that was displayed. When billing is wired
 * up, this is the seam where the SDK will kick off the real purchase flow and report a richer
 * result (success / pending / failure) instead.
 */
interface PurchasePopupCallback {

    /** The user tapped "Confirm Purchase". [data] is the item that was presented. */
    fun onConfirm(data: PurchasePopupData)

    /** The user dismissed the pop-up (Cancel button, back press, or tap outside). */
    fun onCancel()
}
