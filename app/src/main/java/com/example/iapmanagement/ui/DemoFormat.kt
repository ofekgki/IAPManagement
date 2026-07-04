package com.example.iapmanagement.ui

import com.example.iapsdk.models.PurchaseItem
import com.example.iapsdk.models.PurchaseItemType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Small display helpers shared across the demo screens. UI-only formatting, no business logic. */

fun formatPrice(item: PurchaseItem): String =
    if (item.currency.equals("USD", ignoreCase = true)) {
        "$" + String.format(Locale.US, "%.2f", item.price)
    } else {
        String.format(Locale.US, "%.2f %s", item.price, item.currency)
    }

fun typeLabel(type: PurchaseItemType): String = when (type) {
    PurchaseItemType.LIFETIME -> "Lifetime"
    PurchaseItemType.CONSUMABLE -> "Consumable"
    PurchaseItemType.SUBSCRIPTION -> "Subscription"
}

private val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.US)

fun formatDate(epochMillis: Long?): String =
    if (epochMillis == null || epochMillis <= 0L) "—" else dateFormat.format(Date(epochMillis))
