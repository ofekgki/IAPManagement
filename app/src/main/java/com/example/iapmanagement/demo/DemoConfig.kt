package com.example.iapmanagement.demo

/**
 * Central place for the demo app's SDK configuration. In a real app these would come from your
 * build config / a secure source — here they're constants so the demo "just works" after seeding.
 */
object DemoConfig {

    /**
     * Backend SDK API base URL.
     *
     * `10.0.2.2` is the Android emulator's alias for the host machine's `localhost`, so this reaches
     * a backend started with `mvn spring-boot:run` (or docker-compose) on your computer.
     *
     * - Physical device on the same Wi-Fi: replace with `http://<your-computer-LAN-IP>:8080/api/v1/sdk`.
     * - Docker host networking differences may also require the LAN IP.
     */
    const val BACKEND_SDK_BASE_URL = "http://10.0.2.2:8080/api/v1/sdk"

    /** Matches the fixed key created by the backend's SeedDataLoader. DEMO ONLY — never ship a real key. */
    const val API_KEY = "demo_api_key_123"

    /** The default demo end-user the app signs in as automatically (no login needed for the demo). */
    const val DEMO_USER_ID = "demo-user-001"
    const val DEMO_ACCOUNT_ID = "demo-account-001"

    /** The item the "Premium Feature" screen gates access behind. */
    const val PREMIUM_FEATURE_ITEM_ID = "remove_ads"
}
