package com.example.iapmanagement.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.iapmanagement.ui.theme.AccentBlue
import com.example.iapmanagement.ui.theme.AccentPurple
import com.example.iapmanagement.ui.theme.AccentTeal
import com.example.iapmanagement.ui.theme.StatusSuccess
import com.example.iapmanagement.ui.theme.StatusSuccessContainer
import com.example.iapsdk.models.PurchaseItemType
import com.example.iapsdk.models.PurchaseStatus

/** Semantic tone for a status pill, mapped onto the shared palette. */
enum class PillTone { Positive, Negative, Neutral, Accent }

/** A small rounded status chip used across the demo screens for a consistent, modern look. */
@Composable
fun StatusPill(text: String, tone: PillTone, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val bg: Color
    val fg: Color
    when (tone) {
        PillTone.Positive -> {
            bg = StatusSuccessContainer
            fg = StatusSuccess
        }
        PillTone.Negative -> {
            bg = scheme.errorContainer
            fg = scheme.error
        }
        PillTone.Neutral -> {
            bg = scheme.surfaceVariant
            fg = scheme.onSurfaceVariant
        }
        PillTone.Accent -> {
            bg = scheme.secondaryContainer
            fg = scheme.onSecondaryContainer
        }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = fg,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** A distinct accent color per product type, for the store/entitlement badges. */
fun accentForType(type: PurchaseItemType): Color = when (type) {
    PurchaseItemType.LIFETIME -> AccentPurple
    PurchaseItemType.SUBSCRIPTION -> AccentBlue
    PurchaseItemType.CONSUMABLE -> AccentTeal
}

/** Single-letter mark for a product type, shown inside [AccentBadge]. */
fun letterForType(type: PurchaseItemType): String = when (type) {
    PurchaseItemType.LIFETIME -> "L"
    PurchaseItemType.SUBSCRIPTION -> "S"
    PurchaseItemType.CONSUMABLE -> "C"
}

/** A rounded, tinted badge holding a short mark — the shared visual identity across demo cards. */
@Composable
fun AccentBadge(mark: String, accent: Color, modifier: Modifier = Modifier, size: Int = 46) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 3.2f).dp))
            .background(accent.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(mark, color = accent, fontWeight = FontWeight.Bold, fontSize = (size / 2.3f).sp)
    }
}

/** A price shown in a soft accent-tinted chip. */
@Composable
fun PriceChip(text: String, accent: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = accent,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(accent.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 5.dp),
    )
}

/** Maps a purchase status onto a pill tone for restore/purchase listings. */
fun toneForPurchaseStatus(status: PurchaseStatus): PillTone = when (status) {
    PurchaseStatus.SUCCESS -> PillTone.Positive
    PurchaseStatus.RESTORED -> PillTone.Accent
    PurchaseStatus.FAILED -> PillTone.Negative
    else -> PillTone.Neutral
}
