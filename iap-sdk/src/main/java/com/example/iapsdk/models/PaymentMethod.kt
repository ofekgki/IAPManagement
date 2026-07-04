package com.example.iapsdk.models

/**
 * A payment method the user can choose in the purchase popup. [apiValue] is what the backend records
 * (and breaks revenue down by); [displayName] is what the popup shows.
 */
enum class PaymentMethod(val displayName: String, val apiValue: String) {
    CREDIT_CARD("Credit Card", "CREDIT_CARD"),
    APPLE_PAY("Apple Pay", "APPLE_PAY"),
    GOOGLE_PLAY("Google Pay", "GOOGLE_PLAY"),
    PAYPAL("PayPal", "PAYPAL");

    companion object {
        /** The method selected by default when the popup opens. */
        val DEFAULT = CREDIT_CARD
    }
}
