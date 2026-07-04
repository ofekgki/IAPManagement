package com.example.iapsdk.models

/**
 * The throwable the SDK raises when a suspend API cannot return a valid result
 * (e.g. [com.example.iapsdk.PurchaseSdk.getItem] for a missing item, or any call before `init`).
 *
 * It carries a structured [PurchaseSdkError] so callers can inspect the case in a `try/catch`:
 * ```
 * try {
 *     val item = PurchaseSdk.getItem("remove_ads")
 * } catch (e: PurchaseException) {
 *     when (e.error) {
 *         is PurchaseSdkError.ItemNotFound -> /* ... */
 *         else -> /* ... */
 *     }
 * }
 * ```
 */
class PurchaseException(val error: PurchaseSdkError) : Exception(error.message)
