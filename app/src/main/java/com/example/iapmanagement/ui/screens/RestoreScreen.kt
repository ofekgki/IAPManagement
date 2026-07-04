package com.example.iapmanagement.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.iapmanagement.demo.DemoUiState
import com.example.iapmanagement.demo.RestoreState
import com.example.iapmanagement.ui.AccentBadge
import com.example.iapmanagement.ui.PillTone
import com.example.iapmanagement.ui.StatusPill
import com.example.iapmanagement.ui.accentForType
import com.example.iapmanagement.ui.formatPrice
import com.example.iapmanagement.ui.letterForType
import com.example.iapsdk.models.PurchaseItem

/**
 * Restore screen. Lists the demo user's currently-owned items and lets them return one at a time
 * (per-item return) — each return releases that item (it becomes buyable again in the store) and
 * refunds its price from the portal's revenue. A "Return all" shortcut returns everything at once.
 */
@Composable
fun RestoreScreen(
    state: DemoUiState,
    onLoad: () -> Unit,
    onReturnItem: (String) -> Unit,
    onReturnAll: () -> Unit,
) {
    // Load the catalog + owned flags once when the screen appears.
    LaunchedEffect(Unit) { onLoad() }

    val ownedItems = state.items.filter { state.ownedItemIds.contains(it.id) }
    val busy = state.restore is RestoreState.Loading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Return an item to release it (you'll no longer own it, so it's buyable again in the store) " +
                "and refund its price from the portal's revenue. Consumables and items you don't currently " +
                "own aren't returnable.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Owned purchases",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = onLoad, enabled = !busy) { Text("Refresh") }
        }

        when {
            state.itemsLoading && ownedItems.isEmpty() ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text("Loading your purchases…", style = MaterialTheme.typography.bodyMedium)
                    }
                }

            ownedItems.isEmpty() ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Nothing owned to return. Buy an item in the store first.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

            else -> {
                ownedItems.forEach { item ->
                    OwnedItemRow(
                        item = item,
                        returning = state.restoringItemId == item.id,
                        enabled = !busy,
                        onReturn = { onReturnItem(item.id) },
                    )
                }
                if (ownedItems.size >= 2) {
                    OutlinedButton(
                        onClick = onReturnAll,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (busy && state.restoringItemId == null) "Returning all…" else "Return all")
                    }
                }
            }
        }

        RestoreResult(state.restore)
    }
}

@Composable
private fun OwnedItemRow(
    item: PurchaseItem,
    returning: Boolean,
    enabled: Boolean,
    onReturn: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AccentBadge(mark = letterForType(item.type), accent = accentForType(item.type))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${formatPrice(item)} refund",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (returning) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Button(onClick = onReturn, enabled = enabled) { Text("Return") }
            }
        }
    }
}

@Composable
private fun RestoreResult(restore: RestoreState) {
    when (restore) {
        is RestoreState.Idle, is RestoreState.Loading -> Unit

        is RestoreState.Error ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Return failed", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                    Text(restore.message, style = MaterialTheme.typography.bodySmall)
                }
            }

        is RestoreState.Success ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Returned ${restore.results.size} purchase(s)",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (restore.results.isEmpty()) {
                        Text("Nothing owned to return for this user.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        restore.results.forEach { result ->
                            val (label, tone) = when (result.entitlementStatus) {
                                "REVOKED" -> "REVOKED" to PillTone.Negative
                                "EXPIRED" -> "EXPIRED" to PillTone.Neutral
                                else -> "RETURNED" to PillTone.Accent
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = result.itemName ?: result.itemId,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                StatusPill(text = label, tone = tone)
                            }
                        }
                    }
                }
            }
    }
}
