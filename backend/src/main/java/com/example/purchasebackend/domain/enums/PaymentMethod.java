package com.example.purchasebackend.domain.enums;

/**
 * How the user paid for a purchase (the analytics dimension shown in the portal, replacing the old
 * MOCK/GOOGLE_PLAY "billing mode" breakdown).
 *
 * <p>This is independent of {@link BillingMode} (the fulfillment path): in the demo every purchase is
 * fulfilled through MOCK, but the user still picks one of these methods in the SDK popup, and it is
 * recorded per purchase so revenue can be broken down by payment method.
 */
public enum PaymentMethod {
    APPLE_PAY,
    GOOGLE_PLAY,
    PAYPAL,
    CREDIT_CARD
}
