package com.example.iapmanagement

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.iapmanagement.demo.DemoViewModel
import com.example.iapmanagement.ui.DemoApp
import com.example.iapmanagement.ui.theme.IAPManagementTheme
import com.example.iapsdk.PurchaseSdk
import com.example.iapsdk.listener.PurchaseListener
import com.example.iapsdk.models.PurchaseResult
import com.example.iapsdk.models.PurchaseSdkError

/**
 * Hosts the demo UI. The SDK is already initialized in [IapDemoApplication]; here we just render the
 * screens and bridge "Buy"/"Unlock" taps to the SDK's purchase popup.
 *
 * The popup needs an Activity, so [startPurchase] lives here (not in the ViewModel, which must not
 * hold an Activity reference). Purchase outcomes are forwarded back into the [DemoViewModel].
 */
class MainActivity : ComponentActivity() {

    private val viewModel = DemoViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IAPManagementTheme {
                DemoApp(
                    state = viewModel.uiState,
                    vm = viewModel,
                    onBuy = ::startPurchase,
                )
            }
        }
    }

    /**
     * Opens the SDK's custom purchase popup for [itemId]. This is the recommended, batteries-included
     * purchase path — we never bypass the SDK popup. The SDK handles loading the item, showing the
     * popup, confirming/cancelling, running the (mock) purchase, and emitting analytics.
     */
    private fun startPurchase(itemId: String) {
        PurchaseSdk.showPurchasePopup(
            activity = this,
            itemId = itemId,
            listener = object : PurchaseListener {
                override fun onPurchaseSuccess(result: PurchaseResult) = viewModel.onPurchaseSuccess(result)
                override fun onPurchaseCancelled() = viewModel.onPurchaseCancelled()
                override fun onPurchaseFailed(error: PurchaseSdkError) = viewModel.onPurchaseFailed(error)
            },
        )
    }

    override fun onDestroy() {
        viewModel.dispose()
        super.onDestroy()
    }
}
