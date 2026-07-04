package com.example.iapmanagement.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.iapmanagement.demo.DemoConfig
import com.example.iapmanagement.demo.DemoUiState
import com.example.iapmanagement.ui.AccentBadge
import com.example.iapmanagement.ui.PillTone
import com.example.iapmanagement.ui.StatusPill
import com.example.iapmanagement.ui.theme.AccentPurple
import com.example.iapmanagement.ui.theme.StatusSuccess

/**
 * Premium feature screen — the most "real app" flow. It checks entitlement via the SDK's
 * hasEntitlement(...); if the user owns it, the premium content is shown, otherwise a locked state
 * with an "Unlock Now" button that opens the SDK purchase popup for the gating item.
 */
@Composable
fun PremiumScreen(state: DemoUiState, onCheck: () -> Unit, onUnlock: (String) -> Unit) {
    // Re-check access each time the screen is shown (and after returning from a purchase).
    LaunchedEffect(Unit) { onCheck() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Premium Feature: Remove Ads", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "This screen represents a premium-only feature. Access is gated by the SDK call " +
                "hasEntitlement(\"${DemoConfig.PREMIUM_FEATURE_ITEM_ID}\").",
            style = MaterialTheme.typography.bodyMedium,
        )

        when {
            state.premiumLoading -> Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text("Checking access…", modifier = Modifier.padding(top = 12.dp))
                }
            }

            state.premiumUnlocked -> UnlockedContent()

            else -> LockedContent(onUnlock = { onUnlock(DemoConfig.PREMIUM_FEATURE_ITEM_ID) })
        }
    }
}

@Composable
private fun UnlockedContent() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AccentBadge(mark = "✓", accent = StatusSuccess)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusPill(text = "Unlocked", tone = PillTone.Positive)
                    Text("Premium active", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
            }
            Text(
                "Ads are removed and premium content is available. In a real app this is where the " +
                    "premium experience would render.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LockedContent(onUnlock: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AccentBadge(mark = "P", accent = AccentPurple)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusPill(text = "Locked", tone = PillTone.Neutral)
                    Text("Premium locked", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
            }
            Text(
                "You don't own this feature yet. Unlock it to remove ads forever.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onUnlock,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
            ) {
                Text("Unlock Now", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
