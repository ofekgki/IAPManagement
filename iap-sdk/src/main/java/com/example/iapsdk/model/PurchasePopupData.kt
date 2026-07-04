package com.example.iapsdk.model

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes

/**
 * Immutable description of a single purchasable item, used to render the purchase pop-up.
 *
 * This is intentionally a plain, UI-facing model. When the SDK is later connected to the
 * backend, the recommended pattern is to keep this class as the *view* model and map your
 * network DTOs into it (e.g. `PurchaseItemDto.toPopupData()`), so the UI layer stays
 * decoupled from transport details.
 *
 * @property itemName       Human-readable product name shown as the dialog title (e.g. "Remove Ads").
 * @property description    Short marketing/explanatory text shown under the title.
 * @property price          Already-formatted, localized price string (e.g. "$4.99"). Formatting is
 *                          deliberately the caller's responsibility for now; the SDK only displays it.
 * @property paymentMethod  Label of the selected payment method (e.g. "Google Pay").
 * @property accountId      The user/account identifier the purchase is attributed to. Shown in a
 *                          secondary style so the user can confirm they're buying on the right account.
 * @property imageRes       Optional product image as a local drawable resource. Shown at the top of
 *                          the card. Ignored if [imageDrawable] is set. Pass `null` to hide the image.
 * @property imageDrawable  Optional product image as an already-loaded [Drawable]. Takes precedence
 *                          over [imageRes]; this is the seam for remote images — load the URL with
 *                          your own image loader and hand the result in here. `null` to hide.
 * @property disclaimer     Optional consent text. When non-null, a checkbox + this text are shown
 *                          and "Confirm" stays disabled until the user ticks the box. `null` hides
 *                          the checkbox and leaves Confirm always enabled.
 */
data class PurchasePopupData(
    val itemName: String,
    val description: String,
    val price: String,
    val paymentMethod: String,
    val accountId: String,
    @DrawableRes val imageRes: Int? = null,
    val imageDrawable: Drawable? = null,
    val disclaimer: String? = null,
)
