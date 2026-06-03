package com.inventory.industry.ui.components.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.inventory.industry.data.InventoryFlowSummary
import com.inventory.industry.ui.components.cards.SectionCard
import com.inventory.industry.ui.theme.AppSpacing
import com.inventory.industry.ui.theme.AppTypography

private data class QuickBarEntry(val label: String, val value: Float, val color: Color)

@Composable
fun QuickAnalyticsCard(
    flow: InventoryFlowSummary,
    modifier: Modifier = Modifier,
) {
    val entries =
        listOf(
            QuickBarEntry("En proceso", flow.polesInProcessOk.toFloat(), MaterialTheme.colorScheme.primary),
            QuickBarEntry("Terminados", flow.polesReadyStandardSale.toFloat(), Color(0xFF43A047)),
            QuickBarEntry("Fallados", flow.polesFailedSalvage.toFloat(), Color(0xFFE65100)),
        )
    val max = entries.maxOfOrNull { it.value }?.coerceAtLeast(1f) ?: 1f

    SectionCard(
        modifier = modifier.heightIn(max = 220.dp),
        title = "Analítica rápida",
        subtitle = "Postes por estado",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Canvas(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(120.dp),
            ) {
                val n = entries.size
                val gap = 12.dp.toPx()
                val barW = (size.width - gap * (n + 1)) / n
                entries.forEachIndexed { i, e ->
                    val targetH = (e.value / max) * size.height * 0.85f
                    val x = gap + i * (barW + gap)
                    val y = size.height - targetH
                    drawRoundRect(
                        brush =
                            Brush.verticalGradient(
                                colors = listOf(e.color, e.color.copy(alpha = 0.5f)),
                                startY = y,
                                endY = size.height,
                            ),
                        topLeft = Offset(x, y),
                        size = Size(barW, targetH),
                        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                entries.forEach { e ->
                    Text(
                        e.label,
                        style = AppTypography.Caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
