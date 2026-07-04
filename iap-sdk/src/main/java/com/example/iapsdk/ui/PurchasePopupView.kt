package com.example.iapsdk.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.iapsdk.R
import com.example.iapsdk.model.PurchasePopupData
import com.example.iapsdk.models.PaymentMethod
import com.example.iapsdk.models.PurchaseItem
import com.example.iapsdk.models.PurchaseItemType
import com.example.iapsdk.models.PurchaseSdkError
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * The reusable purchase confirmation UI, as a self-contained [android.view.View].
 *
 * Keeping the visual component as a plain View (rather than baking it into a Dialog) means it can
 * be reused in any container: a [PurchaseDialog], a bottom sheet, an embedded panel, a Compose
 * `AndroidView`, etc. [PurchaseDialog] is just the most common, convenient host.
 *
 * The view inflates itself against an SDK-owned Material theme overlay, so it renders correctly
 * even when the host app does not use a Material theme.
 *
 * Bind data with [bind] (UI-level [PurchasePopupData]) or [bindItem] (SDK [PurchaseItem]), and
 * observe button taps via [onConfirm] / [onCancel]. While a purchase is in flight the controller
 * drives [setLoadingState]; failures surface through [showError].
 *
 * Internal: app developers integrate via [com.example.iapsdk.PurchaseSdk.showPurchasePopup] (or the
 * public [PurchaseDialog] facade for the data-only UI), never by touching this view directly.
 */
internal class PurchasePopupView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(themedContext(context), attrs, defStyleAttr) {

    /** Invoked when the user taps "Confirm". Carries the currently bound data. */
    var onConfirm: ((PurchasePopupData) -> Unit)? = null

    /** Invoked when the user taps "Cancel". */
    var onCancel: (() -> Unit)? = null

    /** The payment method the user has selected in the popup (defaults to Credit Card). */
    var selectedPaymentMethod: PaymentMethod = PaymentMethod.DEFAULT
        private set

    private val artworkFrame: FrameLayout
    private val artworkImage: ShapeableImageView
    private val artworkMark: TextView
    private val itemNameView: TextView
    private val descriptionView: TextView
    private val priceView: TextView
    private val paymentMethodView: TextView
    private val paymentRow: View
    private val accountIdView: TextView
    private val termsView: TextView
    private val errorView: TextView
    private val progressView: ProgressBar
    private val confirmButton: MaterialButton
    private val cancelButton: MaterialButton

    private var boundData: PurchasePopupData? = null

    init {
        // Inflate with this view's context (getContext()), which is the themed wrapper passed to
        // super — NOT the constructor `context` parameter, which is the host's (possibly
        // non-Material) context. Material widgets require the themed context to inflate.
        LayoutInflater.from(this.context).inflate(R.layout.iap_view_purchase_popup, this, true)

        artworkFrame = findViewById(R.id.iap_artwork)
        artworkImage = findViewById(R.id.iap_artwork_image)
        artworkMark = findViewById(R.id.iap_artwork_mark)
        itemNameView = findViewById(R.id.iap_text_item_name)
        descriptionView = findViewById(R.id.iap_text_description)
        priceView = findViewById(R.id.iap_text_price)
        paymentMethodView = findViewById(R.id.iap_text_payment_method)
        paymentRow = findViewById(R.id.iap_row_payment)
        accountIdView = findViewById(R.id.iap_text_account_id)
        termsView = findViewById(R.id.iap_text_terms)
        errorView = findViewById(R.id.iap_text_error)
        progressView = findViewById(R.id.iap_progress)
        confirmButton = findViewById(R.id.iap_button_confirm)
        cancelButton = findViewById(R.id.iap_button_cancel)

        confirmButton.setOnClickListener {
            boundData?.let { data -> onConfirm?.invoke(data) }
        }
        cancelButton.setOnClickListener {
            onCancel?.invoke()
        }
        // Tapping the payment row lets the user choose how to pay.
        paymentRow.setOnClickListener { showPaymentMethodChooser() }
    }

    /** Opens a single-choice dialog to pick the payment method, then updates the row label. */
    private fun showPaymentMethodChooser() {
        val methods = PaymentMethod.entries.toTypedArray()
        val labels = methods.map { it.displayName }.toTypedArray()
        val checked = methods.indexOf(selectedPaymentMethod)
        MaterialAlertDialogBuilder(this.context)
            .setTitle(R.string.iap_payment_method_title)
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                selectedPaymentMethod = methods[which]
                paymentMethodView.text = selectedPaymentMethod.displayName
                dialog.dismiss()
            }
            .show()
    }

    /** Populates the pop-up with [data]. Safe to call multiple times to re-bind. */
    fun bind(data: PurchasePopupData) {
        boundData = data

        // Artwork: a caller-supplied image wins; otherwise we generate a cohesive gradient tile with
        // a monogram from the product name (bindItem later re-tints it per product type).
        when {
            data.imageDrawable != null -> showImageArtwork { setImageDrawable(data.imageDrawable) }
            data.imageRes != null -> showImageArtwork { setImageResource(data.imageRes) }
            else -> showGeneratedArtwork(
                monogramOf(data.itemName),
                colorOf(R.color.iap_primary),
                colorOf(R.color.iap_accent),
            )
        }

        itemNameView.text = data.itemName
        descriptionView.text = data.description
        priceView.text = data.price
        paymentMethodView.text = data.paymentMethod
        accountIdView.text = data.accountId

        // Disclaimer is optional, shown as fine-print terms beneath the rows.
        val disclaimer = data.disclaimer
        termsView.isVisible = disclaimer != null
        termsView.text = disclaimer

        clearError()
    }

    /**
     * Binds an SDK [PurchaseItem] for the given [userId], formatting the price from
     * [PurchaseItem.price]/[PurchaseItem.currency] and using a generic payment-method placeholder.
     * This is the entry point used by the high-level [com.example.iapsdk.PurchaseSdk] flow.
     */
    fun bindItem(item: PurchaseItem, userId: String?) {
        selectedPaymentMethod = PaymentMethod.DEFAULT
        bind(
            PurchasePopupData(
                itemName = item.name,
                description = item.description,
                price = formatPrice(item.price, item.currency),
                paymentMethod = selectedPaymentMethod.displayName,
                accountId = userId ?: GUEST_ACCOUNT_LABEL,
            ),
        )
        // Re-tint the artwork with a gradient cohesive with this product's type.
        val (start, end) = artColorsForType(item.type)
        showGeneratedArtwork(monogramOf(item.name), start, end)
    }

    /** Shows a caller-supplied image in the artwork tile (hiding the generated monogram). */
    private fun showImageArtwork(apply: ShapeableImageView.() -> Unit) {
        artworkImage.apply(apply)
        artworkImage.isVisible = true
        artworkMark.isVisible = false
        artworkFrame.background = null
    }

    /** Draws a rounded gradient tile with a centered [mark] — the cohesive per-product artwork. */
    private fun showGeneratedArtwork(mark: String, startColor: Int, endColor: Int) {
        artworkImage.isVisible = false
        artworkMark.text = mark
        artworkMark.isVisible = true
        artworkFrame.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(startColor, endColor),
        ).apply { cornerRadius = resources.getDimension(R.dimen.iap_artwork_corner) }
    }

    /** First letter of the product name (uppercased), used as the artwork monogram. */
    private fun monogramOf(name: String): String =
        (name.firstOrNull { it.isLetter() } ?: name.trim().firstOrNull())?.uppercase() ?: "?"

    private fun colorOf(resId: Int): Int = ContextCompat.getColor(context, resId)

    /** A gradient (start, end) cohesive with the product type — matches the demo app's card badges. */
    private fun artColorsForType(type: PurchaseItemType): Pair<Int, Int> = when (type) {
        PurchaseItemType.LIFETIME ->
            colorOf(R.color.iap_art_lifetime_start) to colorOf(R.color.iap_art_lifetime_end)
        PurchaseItemType.SUBSCRIPTION ->
            colorOf(R.color.iap_art_subscription_start) to colorOf(R.color.iap_art_subscription_end)
        PurchaseItemType.CONSUMABLE ->
            colorOf(R.color.iap_art_consumable_start) to colorOf(R.color.iap_art_consumable_end)
    }

    /** Toggles the in-flight state: shows a spinner and locks the buttons so Confirm can't re-fire. */
    fun setLoadingState(isLoading: Boolean) {
        progressView.isVisible = isLoading
        confirmButton.isEnabled = !isLoading
        cancelButton.isEnabled = !isLoading
        if (isLoading) clearError()
    }

    /** Surfaces a [PurchaseSdkError] as inline text beneath the rows and clears any loading state. */
    fun showError(error: PurchaseSdkError) {
        setLoadingState(false)
        errorView.text = error.message
        errorView.isVisible = true
    }

    /** Hides the inline error text. */
    fun clearError() {
        errorView.text = null
        errorView.isVisible = false
    }

    private fun formatPrice(price: Double, currencyCode: String): String =
        runCatching {
            NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
                currency = Currency.getInstance(currencyCode)
            }.format(price)
        }.getOrDefault("%.2f %s".format(price, currencyCode))

    private companion object {
        const val PAYMENT_METHOD_PLACEHOLDER = "Default payment method"
        const val GUEST_ACCOUNT_LABEL = "Guest"

        /** Wraps the host context in the SDK's Material theme so Material widgets resolve correctly. */
        fun themedContext(context: Context): Context =
            ContextThemeWrapper(context, R.style.Theme_Iap_Dialog)
    }
}
