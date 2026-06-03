package com.inventory.industry.ui.components.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inventory.industry.data.StageInventoryRow
import com.inventory.industry.ui.components.cards.SectionCard
import com.inventory.industry.ui.formatQty
import com.inventory.industry.ui.theme.AppShapes
import com.inventory.industry.ui.theme.AppSpacing
import com.inventory.industry.ui.theme.AppTypography

@Composable
fun CompactStageProgressCard(
    rows: List<StageInventoryRow>,
    modifier: Modifier = Modifier,
) {
    SectionCard(
        modifier = modifier.heightIn(max = 280.dp),
        title = "Avance por etapa",
        subtitle = "OK vs fallados",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            rows.forEach { row ->
                CompactStageRow(row = row)
            }
        }
    }
}

@Composable
private fun CompactStageRow(row: StageInventoryRow) {
    val total = row.okPoles + row.failedPoles
    val pct =
        if (total > 1e-6) {
            (row.okPoles / total).toFloat().coerceIn(0f, 1f)
        } else {
            1f
        }
    val animated by animateFloatAsState(targetValue = pct, animationSpec = tween(400), label = "stagePct")
    Column(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                row.stage.shortCode,
                style = AppTypography.BodySmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${(animated * 100).toInt()}% OK · ${formatQty(row.totalPoles)} u.",
                style = AppTypography.Caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { animated },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(AppShapes.small),
        )
    }
}
