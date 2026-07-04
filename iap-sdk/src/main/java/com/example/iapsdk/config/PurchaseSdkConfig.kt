package com.example.iapsdk.config

/**
 * Target environment for the SDK's (future) backend calls. For now it only influences which mock
 * base URL the internal `ApiClient` reports; no real network call is made yet.
 */
enum class SdkEnvironment {
    /** Test environment — safe for development; no real charges. */
    SANDBOX,

    /** Live environment — real users and (eventually) real charges. */
    PRODUCTION,
}

/**
 * Optional configuration passed to [com.example.iapsdk.PurchaseSdk.init].
 *
 * All values have sensible defaults, so `PurchaseSdkConfig()` is a valid configuration.
 *
 * @property environment      Which backend environment to target (see [SdkEnvironment]).
 * @property baseUrl          Overrides the default base URL for the chosen [environment]. `null`
 *                            means "use the environment's default" (resolved by `ApiClient`).
 * @property enableAnalytics  When false, the internal `AnalyticsTracker` drops all events.
 * @property enableLogs       When false, the SDK suppresses its debug logging.
 * @property enableLocalCache When false, items/entitlements are always fetched fresh instead of
 *                            being served from / written to local storage.
 */
data class PurchaseSdkConfig(
    val environment: SdkEnvironment = SdkEnvironment.SANDBOX,
    val baseUrl: String? = null,
    val enableAnalytics: Boolean = true,
    val enableLogs: Boolean = true,
    val enableLocalCache: Boolean = true,
)
