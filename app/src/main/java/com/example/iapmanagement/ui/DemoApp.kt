package com.example.iapmanagement.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.iapmanagement.demo.DemoUiState
import com.example.iapmanagement.demo.DemoViewModel
import com.example.iapmanagement.ui.theme.AccentTealDark
import com.example.iapmanagement.ui.theme.BrandPrimary
import com.example.iapmanagement.ui.theme.BrandPrimaryDark
import com.example.iapmanagement.ui.screens.EntitlementsScreen
import com.example.iapmanagement.ui.screens.HomeScreen
import com.example.iapmanagement.ui.screens.PremiumScreen
import com.example.iapmanagement.ui.screens.RestoreScreen
import com.example.iapmanagement.ui.screens.StoreScreen

/** The demo's screens. A tiny hand-rolled router keeps the demo dependency-free (no Nav component). */
enum class DemoDest(val title: String) {
    Home("IAP SDK Demo"),
    Store("Store"),
    Entitlements("My Entitlements"),
    Restore("Restore Purchases"),
    Premium("Premium Feature"),
}

/**
 * Root composable. Owns the current destination, renders a simple top bar + one-shot message banner,
 * and dispatches to the active screen. All data comes from [state]; all actions go through [vm] or the
 * [onBuy] callback (which opens the SDK popup from the Activity).
 */
@Composable
fun DemoApp(state: DemoUiState, vm: DemoViewModel, onBuy: (String) -> Unit) {
    var dest by remember { mutableStateOf(DemoDest.Home) }

    // Android system back: return to Home from any sub-screen instead of leaving the app.
    BackHandler(enabled = dest != DemoDest.Home) { dest = DemoDest.Home }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                title = dest.title,
                showBack = dest != DemoDest.Home,
                onBack = { dest = DemoDest.Home },
            )

            state.message?.let { message ->
                MessageBanner(message = message, onDismiss = vm::clearMessage)
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (dest) {
                    DemoDest.Home -> HomeScreen(onNavigate = { dest = it })
                    DemoDest.Store -> StoreScreen(state = state, onRefresh = vm::loadStore, onBuy = onBuy)
                    DemoDest.Entitlements -> EntitlementsScreen(state = state, onRefresh = vm::loadEntitlements)
                    DemoDest.Restore -> RestoreScreen(
                        state = state,
                        onLoad = vm::loadStore,
                        onReturnItem = vm::returnItem,
                        onReturnAll = vm::restorePurchases,
                    )
                    DemoDest.Premium -> PremiumScreen(
                        state = state,
                        onCheck = { vm.checkPremiumAccess() },
                        onUnlock = onBuy,
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(title: String, showBack: Boolean, onBack: () -> Unit) {
    Surface(shadowElevation = 4.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // A subtle brand gradient gives the bar a more distinctive, modern feel.
                .background(Brush.horizontalGradient(listOf(BrandPrimary, BrandPrimaryDark, AccentTealDark)))
                // Push the bar's content below the system status bar (edge-to-edge is enabled), so the
                // title/back button aren't hidden under the status bar / notification pull-down.
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showBack) {
                // Circular, comfortably tappable back affordance.
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "‹", // single left-angle chevron (not an emoji)
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
            } else {
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun MessageBanner(message: String, onDismiss: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}
