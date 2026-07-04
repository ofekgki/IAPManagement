package com.example.iapsdk.ui

import android.app.Activity
import android.app.Dialog
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import com.example.iapsdk.R
import com.example.iapsdk.analytics.AnalyticsTracker
import com.example.iapsdk.error.ErrorMapper
import com.example.iapsdk.listener.PurchaseListener
import com.example.iapsdk.models.PurchaseException
import com.example.iapsdk.models.PurchaseItem
import com.example.iapsdk.purchase.PurchaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Hosts the internal [PurchasePopupView] in a bottom-sheet [Dialog] and connects user actions to the
 * purchase flow. This keeps all popup orchestration out of the public [com.example.iapsdk.PurchaseSdk]
 * facade, which just forwards to [showPopup].
 *
 * On Confirm: locks the view, runs [PurchaseManager.makePurchase], then either dismisses and reports
 * [PurchaseListener.onPurchaseSuccess], or shows the error inline and reports
 * [PurchaseListener.onPurchaseFailed] (leaving the sheet open so the user can retry or cancel).
 *
 * > Design note: the spec sketches "add the view to the activity root". A self-themed [Dialog] is
 * > used instead — it gives a correct modal scrim, back-press handling, and window lifecycle for
 * > free, and matches the existing [PurchaseDialog] presentation.
 */
internal class PurchasePopupController(
    private val purchaseManager: PurchaseManager,
    private val analytics: AnalyticsTracker,
) {

    private var dialog: Dialog? = null

    /** Whether a popup is currently on screen. */
    fun isPopupShowing(): Boolean = dialog?.isShowing == true

    /** Builds, wires, and shows the popup for [item]; routes the outcome to [listener]. */
    fun showPopup(
        activity: Activity,
        item: PurchaseItem,
        userId: String?,
        listener: PurchaseListener,
    ) {
        // Only one popup at a time.
        dismissPopup()

        // Main-thread scope tied to this popup; cancelled when the dialog goes away.
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        // Guards against firing more than one terminal callback (e.g. back press after success).
        var resolved = false

        val dialog = Dialog(activity, R.style.Theme_Iap_Dialog)
        val popupView = PurchasePopupView(activity)
        popupView.bindItem(item, userId)

        fun teardown() {
            scope.cancel()
            dialog.dismiss()
            this.dialog = null
        }

        popupView.onConfirm = {
            if (!resolved) {
                analytics.trackPopupConfirmed(item.id)
                popupView.setLoadingState(true)
                scope.launch {
                    try {
                        // Confirm delegates to the active BillingProvider (mock or Google Play).
                        // Google Play Billing always shows the official Google purchase UI.
                        // This SDK popup is only a pre-purchase UI and does not replace Google's
                        // final payment screen.
                        val result = purchaseManager.makePurchase(
                            item.id, popupView.selectedPaymentMethod.apiValue)
                        resolved = true
                        teardown()
                        listener.onPurchaseSuccess(result)
                    } catch (e: PurchaseException) {
                        // Keep the sheet open so the user can retry or cancel. In GOOGLE_PLAY mode
                        // (until configured) this surfaces GooglePlayNotConfigured here.
                        popupView.showError(e.error)
                        listener.onPurchaseFailed(e.error)
                    } catch (t: Throwable) {
                        val error = ErrorMapper.mapThrowableToSdkError(t)
                        popupView.showError(error)
                        listener.onPurchaseFailed(error)
                    }
                }
            }
        }
        popupView.onCancel = {
            if (!resolved) {
                resolved = true
                analytics.trackPopupCancelled(item.id)
                teardown()
                listener.onPurchaseCancelled()
            }
        }

        dialog.setContentView(popupView)
        dialog.setCancelable(true)
        // Back press / outside tap → treat as cancel (our own dismiss() calls don't trigger this).
        dialog.setOnCancelListener {
            if (!resolved) {
                resolved = true
                analytics.trackPopupCancelled(item.id)
                scope.cancel()
                this.dialog = null
                listener.onPurchaseCancelled()
            }
        }

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            setGravity(Gravity.BOTTOM)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.55f)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        this.dialog = dialog
        analytics.trackPopupShown(item.id)
        dialog.show()
    }

    /** Dismisses any visible popup without invoking listener callbacks. */
    fun dismissPopup() {
        dialog?.dismiss()
        dialog = null
    }
}
