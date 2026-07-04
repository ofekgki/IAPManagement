package com.example.iapsdk.models

/**
 * The commercial nature of a [PurchaseItem]. Drives entitlement semantics (e.g. whether an
 * entitlement can expire) and, later, which billing flow the SDK will use.
 */
enum class PurchaseItemType {
    /** Bought once, owned forever (e.g. "Remove Ads"). */
    LIFETIME,

    /** Can be bought repeatedly and is "used up" (e.g. a coin pack). */
    CONSUMABLE,

    /** Time-limited access that renews/expires (e.g. a monthly plan). */
    SUBSCRIPTION,
}
