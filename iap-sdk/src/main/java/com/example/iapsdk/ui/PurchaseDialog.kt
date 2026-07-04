package com.example.iapsdk.ui

import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import com.example.iapsdk.R
import com.example.iapsdk.callbacks.PurchasePopupCallback
import com.example.iapsdk.model.PurchasePopupData

/**
 * Developer-facing entry point for showing the purchase confirmation pop-up.
 *
 * This is the convenience facade over [PurchasePopupView]: it hosts the view in a centered,
 * rounded [Dialog] and translates user actions into simple callbacks.
 *
 * Typical Kotlin usage:
 * ```
 * PurchaseDialog.show(
 *     context = this,
 *     data = PurchasePopupData(
 *         itemName = "Remove Ads",
 *         description = "Enjoy the app without ads forever.",
 *         price = "$4.99",
 *         paymentMethod = "Google Pay",
 *         accountId = "user_12345",
 *     ),
 *     onConfirm = { purchaseData -> /* start the real purchase later */ },
 *     onCancel = { /* user backed out */ },
 * )
 * ```
 *
 * For now [onConfirm] simply returns the displayed [PurchasePopupData]; no billing or network
 * call happens. This is the intended integration seam for the future SDK purchase flow.
 */
object PurchaseDialog {

    /**
     * Shows the purchase pop-up using lambda callbacks (idiomatic Kotlin).
     *
     * @param context     A context tied to a visible Activity (a Dialog needs an Activity window).
     * @param data        The item to present.
     * @param cancelable  Whether back press / tapping the scrim dismisses the dialog (counts as cancel).
     * @param onConfirm   Called once if the user confirms; receives the presented [data].
     * @param onCancel    Called once if the user cancels (button, back press, or outside tap).
     * @return The shown [Dialog], in case the caller wants to dismiss it programmatically.
     */
    @JvmStatic
    @JvmOverloads
    fun show(
        context: Context,
        data: PurchasePopupData,
        cancelable: Boolean = true,
        onConfirm: (PurchasePopupData) -> Unit,
        onCancel: () -> Unit = {},
    ): Dialog {
        val dialog = Dialog(context, R.style.Theme_Iap_Dialog)

        // Guards against double-reporting: tapping Confirm/Cancel dismisses the dialog, and we must
        // not let the resulting dismissal also fire the "cancel" path.
        var resolved = false

        // Assigned without `apply` on purpose: an implicit receiver here would let the view's
        // onConfirm/onCancel properties shadow the function parameters of the same name.
        val popupView = PurchasePopupView(context)
        popupView.bind(data)
        popupView.onConfirm = { confirmedData ->
            if (!resolved) {
                resolved = true
                onConfirm(confirmedData)
                dialog.dismiss()
            }
        }
        popupView.onCancel = {
            if (!resolved) {
                resolved = true
                onCancel()
                dialog.dismiss()
            }
        }

        dialog.setContentView(popupView)
        dialog.setCancelable(cancelable)
        // Fires for back press / outside tap (not for our explicit dismiss() calls).
        dialog.setOnCancelListener {
            if (!resolved) {
                resolved = true
                onCancel()
            }
        }

        dialog.window?.apply {
            // Transparent so the card's rounded corners and margins are visible over the scrim.
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            // Anchor to the bottom like a sheet (top corners are rounded by the card's shape).
            setGravity(Gravity.BOTTOM)
            // Non-floating windows don't dim behind automatically, so request it explicitly to get
            // the modal scrim. Dim amount mirrors the theme's backgroundDimAmount.
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.55f)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        dialog.show()
        return dialog
    }

    /**
     * Java-friendly overload using the [PurchasePopupCallback] interface.
     */
    @JvmStatic
    @JvmOverloads
    fun show(
        context: Context,
        data: PurchasePopupData,
        callback: PurchasePopupCallback,
        cancelable: Boolean = true,
    ): Dialog = show(
        context = context,
        data = data,
        cancelable = cancelable,
        onConfirm = { callback.onConfirm(it) },
        onCancel = { callback.onCancel() },
    )
}
