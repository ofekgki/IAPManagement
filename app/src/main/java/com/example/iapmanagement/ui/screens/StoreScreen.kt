package com.example.iapmanagement.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.iapmanagement.demo.DemoUiState
import com.example.iapmanagement.ui.AccentBadge
import com.example.iapmanagement.ui.PriceChip
import com.example.iapmanagement.ui.accentForType
import com.example.iapmanagement.ui.formatPrice
import com.example.iapmanagement.ui.letterForType
import com.example.iapmanagement.ui.theme.StatusSuccess
import com.example.iapmanagement.ui.theme.StatusSuccessContainer
import com.example.iapmanagement.ui.typeLabel
import com.example.iapsdk.models.PurchaseItem

/**
 * Store screen. Loads the catalog from the backend (via the SDK) on first display and lists each
 * product on a modern, color-coded card with a Buy button that opens the SDK's purchase popup.
 */
@Composable
fun StoreScreen(state: DemoUiState, onRefresh: () -> Unit, onBuy: (String) -> Unit) {
    // Load once when the screen appears (and never on every recomposition).
    LaunchedEffect(Unit) { onRefresh() }

    when {
        state.itemsLoading && state.items.isEmpty() -> CenteredLoader()
        state.itemsError != null && state.items.isEmpty() ->
            CenteredMessage("Couldn't load items.\n${state.itemsError}\n\nIs the backend running?", onRefresh)
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("Store", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "${state.items.size} product${if (state.items.size == 1) "" else "s"} · MOCK billing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onRefresh) { Text("Refresh") }
                }
            }
            items(state.items, key = { it.id }) { item ->
                StoreItemCard(
                    item = item,
                    owned = state.ownedItemIds.contains(item.id),
                    onBuy = { onBuy(item.id) },
                )
            }
        }
    }
}

@Composable
private fun StoreItemCard(item: PurchaseItem, owned: Boolean, onBuy: () -> Unit) {
    val accent = accentForType(item.type)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AccentBadge(mark = letterForType(item.type), accent = accent)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${typeLabel(item.type)} · ${item.currency}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                PriceChip(text = formatPrice(item), accent = accent)
            }

            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (owned) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(StatusSuccessContainer)
                        .height(44.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Owned", color = StatusSuccess, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onBuy,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text("Buy  ·  ${formatPrice(item)}", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun CenteredLoader() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Text("Loading items…", modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun CenteredMessage(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) { Text("Retry") }
    }
}
