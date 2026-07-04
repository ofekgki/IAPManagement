package com.example.iapmanagement

import android.app.Application
import com.example.iapmanagement.demo.DemoConfig
import com.example.iapsdk.PurchaseSdk
import com.example.iapsdk.config.BillingMode
import com.example.iapsdk.config.PurchaseSdkConfig

/**
 * Demo app entry point. Initializes the IAP SDK once, at process start — exactly how a real host app
 * would do it from `Application.onCreate`.
 *
 * The SDK runs in [BillingMode.MOCK]: purchases are simulated end-to-end against the backend (real
 * rows are written to the database), but no real payment happens. Switching to
 * [BillingMode.GOOGLE_PLAY] currently fails safely until the GooglePlayBillingProvider TODOs are done.
 */
class IapDemoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // STEP 1 of the demo flow: initialize the SDK with the API key, the demo user, and the
        // backend base URL. Only the application context is retained by the SDK.
        if (!PurchaseSdk.isInitialized()) {
            PurchaseSdk.init(
                context = this,
                apiKey = DemoConfig.API_KEY,
                billingMode = BillingMode.MOCK,
                userId = DemoConfig.DEMO_USER_ID,
                config = PurchaseSdkConfig(
                    baseUrl = DemoConfig.BACKEND_SDK_BASE_URL,
                    enableLogs = true,
                ),
            )
        }
    }
}
