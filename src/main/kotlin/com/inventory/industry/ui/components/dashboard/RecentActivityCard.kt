package com.inventory.industry.ui.components.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inventory.industry.data.DashboardActivityEntry
import com.inventory.industry.data.DashboardActivityKind
import com.inventory.industry.ui.components.cards.SectionCard
import com.inventory.industry.ui.formatEpochTimeShort
import com.inventory.industry.ui.theme.AppSpacing
import com.inventory.industry.ui.theme.AppTypography

@Composable
fun RecentActivityCard(
    entries: List<DashboardActivityEntry>,
    modifier: Modifier = Modifier,
    maxItems: Int = 5,
) {
    SectionCard(
        modifier = modifier.heightIn(max = 280.dp),
        title = "Actividad reciente",
        subtitle = "Últimos movimientos",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            entries.take(maxItems).forEach { entry ->
                CompactActivityRow(entry = entry)
            }
            if (entries.isEmpty()) {
                Text(
                    "Sin actividad reciente",
                    style = AppTypography.BodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CompactActivityRow(entry: DashboardActivityEntry) {
    val (icon, tint) = activityIcon(entry.kind)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Text(
            entry.title,
            style = AppTypography.BodySmall,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            formatEpochTimeShort(entry.epochMs),
            style = AppTypography.Caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun activityIcon(kind: DashboardActivityKind): Pair<ImageVector, Color> =
    when (kind) {
        DashboardActivityKind.Sale -> Icons.Outlined.ShoppingCart to Color(0xFF2E7D52)
        DashboardActivityKind.Transformation -> Icons.Default.ShowChart to Color(0xFF3949AB)
        DashboardActivityKind.Transport -> Icons.Default.LocalShipping to Color(0xFF1565C0)
        DashboardActivityKind.Inventory -> Icons.Outlined.History to Color(0xFF6D4C41)
    }
