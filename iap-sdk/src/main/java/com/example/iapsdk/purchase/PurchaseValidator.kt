package com.example.iapsdk.purchase

import com.example.iapsdk.models.PurchaseException
import com.example.iapsdk.models.PurchaseSdkError

/**
 * Centralized precondition checks. Each throws a [PurchaseException] carrying the matching
 * [PurchaseSdkError], so failures funnel through the same error contract as the rest of the SDK.
 */
internal object PurchaseValidator {

    fun validateInitialized(isInitialized: Boolean) {
        if (!isInitialized) fail(PurchaseSdkError.NotInitialized)
    }

    fun validateApiKey(apiKey: String) {
        // TODO(backend): replace this blank-check with real API-key validation against the backend.
        if (apiKey.isBlank()) fail(PurchaseSdkError.Unknown("API key must not be blank."))
    }

    fun validateItemId(itemId: String) {
        if (itemId.isBlank()) fail(PurchaseSdkError.ItemNotFound)
    }

    fun validatePrice(price: Double) {
        if (price < 0.0) fail(PurchaseSdkError.Unknown("Item price must not be negative (was $price)."))
    }

    private fun fail(error: PurchaseSdkError): Nothing = throw PurchaseException(error)
}
