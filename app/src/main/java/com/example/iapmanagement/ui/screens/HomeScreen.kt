package com.example.iapmanagement.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.iapmanagement.demo.DemoConfig
import com.example.iapmanagement.ui.DemoDest
import com.example.iapmanagement.ui.theme.AccentBlue
import com.example.iapmanagement.ui.theme.AccentPurple
import com.example.iapmanagement.ui.theme.AccentTeal
import com.example.iapmanagement.ui.theme.BrandPrimary
import com.example.iapmanagement.ui.theme.BrandPrimaryDark
import com.example.iapmanagement.ui.theme.StatusWarning

/**
 * Demo home screen. A gradient hero introduces the app; distinct, color-coded cards are the "menu"
 * into each SDK flow. Kept dependency-free (no icon pack): section marks are drawn as tinted badges.
 */
@Composable
fun HomeScreen(onNavigate: (DemoDest) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Hero()

        SectionCard(
            mark = "S",
            accent = AccentTeal,
            title = "Store",
            subtitle = "Load products from the backend and buy with the SDK popup.",
            onClick = { onNavigate(DemoDest.Store) },
        )
        SectionCard(
            mark = "E",
            accent = AccentBlue,
            title = "My Entitlements",
            subtitle = "What the demo user currently owns or is subscribed to.",
            onClick = { onNavigate(DemoDest.Entitlements) },
        )
        SectionCard(
            mark = "R",
            accent = StatusWarning,
            title = "Restore / Return",
            subtitle = "Return owned purchases and see them refunded in the portal.",
            onClick = { onNavigate(DemoDest.Restore) },
        )
        SectionCard(
            mark = "P",
            accent = AccentPurple,
            title = "Premium Feature",
            subtitle = "Gate a feature behind an entitlement; unlock it via a purchase.",
            onClick = { onNavigate(DemoDest.Premium) },
        )

        SessionCard()
    }
}

@Composable
private fun Hero() {
    Surface(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(BrandPrimary, BrandPrimaryDark, AccentTeal)))
                .padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "MOCK BILLING · NO REAL CHARGES",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
                Text(
                    text = "In-App Purchase SDK",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = "A sample app driving the custom IAP SDK end-to-end: browse products, buy " +
                        "through the SDK popup, return purchases, check entitlements, and stream " +
                        "analytics to the developer portal.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.92f),
                )
            }
        }
    }
}

@Composable
private fun SectionCard(mark: String, accent: Color, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(mark, color = accent, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(50))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("›", color = accent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun SessionCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Demo session", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            InfoLine("User", DemoConfig.DEMO_USER_ID)
            InfoLine("Account", DemoConfig.DEMO_ACCOUNT_ID)
            InfoLine("Backend", DemoConfig.BACKEND_SDK_BASE_URL)
            Text(
                "Item views, popup opens, purchases, returns, and entitlement checks stream to the " +
                    "backend automatically and appear in the developer portal.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "$label:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}
