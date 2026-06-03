package com.inventory.industry.ui.components.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.inventory.industry.data.InventoryFlowSummary
import com.inventory.industry.ui.components.cards.SectionCard
import com.inventory.industry.ui.formatQty
import com.inventory.industry.ui.theme.AppSpacing
import com.inventory.industry.ui.theme.AppTypography

@Composable
fun InventoryDonutCard(
    flow: InventoryFlowSummary,
    modifier: Modifier = Modifier,
) {
    val segments =
        listOf(
            Triple("Listos", flow.polesReadyStandardSale.toFloat(), Color(0xFF43A047)),
            Triple("En proceso", flow.polesInProcessOk.toFloat(), MaterialTheme.colorScheme.primary),
            Triple("Fallados", flow.polesFailedSalvage.toFloat(), Color(0xFFE65100)),
        )
    val total = segments.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(1f)
    val sweepAnim by animateFloatAsState(1f, tween(700), label = "donutSweep")

    SectionCard(
        modifier = modifier.heightIn(max = 220.dp),
        title = "Estado inventario",
        subtitle = "Distribución actual",
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(180.dp)) {
                    val stroke = 22.dp.toPx()
                    val r = (size.minDimension - stroke) / 2f
                    val c = Offset(size.width / 2f, size.height / 2f)
                    var start = -90f
                    segments.forEach { (_, value, color) ->
                        val sweep = (value / total) * 360f * sweepAnim
                        drawArc(
                            color = color,
                            startAngle = start,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = Offset(c.x - r, c.y - r),
                            size = Size(r * 2, r * 2),
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                        start += sweep
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                segments.forEach { (label, value, color) ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(color),
                        )
                        Column {
                            Text(label, style = AppTypography.BodySmall)
                            Text(
                                formatQty(value.toDouble()),
                                style = AppTypography.Caption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
