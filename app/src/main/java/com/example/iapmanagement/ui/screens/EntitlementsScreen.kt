package com.example.iapmanagement.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.iapmanagement.demo.DemoUiState
import com.example.iapmanagement.ui.AccentBadge
import com.example.iapmanagement.ui.PillTone
import com.example.iapmanagement.ui.StatusPill
import com.example.iapmanagement.ui.accentForType
import com.example.iapmanagement.ui.formatDate
import com.example.iapmanagement.ui.letterForType
import com.example.iapmanagement.ui.typeLabel
import com.example.iapsdk.models.UserEntitlement

/**
 * Entitlements screen. Uses the SDK's listEntitlements() to show what the demo user owns, with a
 * clear empty state when there's nothing.
 */
@Composable
fun EntitlementsScreen(state: DemoUiState, onRefresh: () -> Unit) {
    LaunchedEffect(Unit) { onRefresh() }

    when {
        state.entitlementsLoading && state.entitlements.isEmpty() ->
            Centered { CircularProgressIndicator(); Text("Loading entitlements…", Modifier.padding(top = 12.dp)) }

        state.entitlementsError != null ->
            Centered {
                Text("Couldn't load entitlements.\n${state.entitlementsError}", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onRefresh, modifier = Modifier.padding(top = 16.dp)) { Text("Retry") }
            }

        state.entitlements.isEmpty() ->
            Centered {
                Text("No active entitlements yet.", style = MaterialTheme.typography.titleMedium)
                Text(
                    "If you just revoked one in the portal, it will drop off here after a refresh.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Button(onClick = onRefresh, modifier = Modifier.padding(top = 16.dp)) { Text("Refresh") }
            }

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Active entitlements",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    TextButton(onClick = onRefresh) { Text("Refresh") }
                }
            }
            items(state.entitlements, key = { it.itemId }) { EntitlementCard(it) }
        }
    }
}

@Composable
private fun EntitlementCard(entitlement: UserEntitlement) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AccentBadge(mark = letterForType(entitlement.type), accent = accentForType(entitlement.type))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = entitlement.itemId,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = typeLabel(entitlement.type),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusPill(
                    text = if (entitlement.isActive) "ACTIVE" else "INACTIVE",
                    tone = if (entitlement.isActive) PillTone.Positive else PillTone.Negative,
                )
            }
            val muted = MaterialTheme.colorScheme.onSurfaceVariant
            Text("Granted: ${formatDate(entitlement.grantedAt)}", style = MaterialTheme.typography.bodySmall, color = muted)
            Text(
                text = "Expires: " + (entitlement.expiresAt?.let { formatDate(it) } ?: "never (lifetime)"),
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = { content() },
    )
}
