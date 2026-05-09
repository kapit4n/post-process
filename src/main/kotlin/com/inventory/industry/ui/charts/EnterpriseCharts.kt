package com.inventory.industry.ui.charts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.inventory.industry.ui.theme.AppSpacing
import com.inventory.industry.ui.theme.AppTypography

data class BarChartEntry(
    val label: String,
    val value: Float,
    val color: Color,
)

@Composable
fun EnterpriseBarChart(
    title: String,
    entries: List<BarChartEntry>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 200.dp,
) {
    val max = entries.maxOfOrNull { it.value }?.coerceAtLeast(1f) ?: 1f
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Text(title, style = AppTypography.CardTitle, color = MaterialTheme.colorScheme.onSurface)
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
                    .padding(vertical = AppSpacing.sm),
        ) {
            val n = entries.size.coerceAtLeast(1)
            val gap = 8.dp.toPx()
            val barW = (size.width - gap * (n + 1)) / n
            entries.forEachIndexed { i, e ->
                val targetH = (e.value / max) * size.height * 0.88f
                val x = gap + i * (barW + gap)
                val y = size.height - targetH
                drawRoundRect(
                    brush =
                        Brush.verticalGradient(
                            colors = listOf(e.color, e.color.copy(alpha = 0.55f)),
                            startY = y,
                            endY = size.height,
                        ),
                    topLeft = Offset(x, y),
                    size = Size(barW, targetH),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                )
            }
        }
    }
}

data class DonutSegment(
    val label: String,
    val value: Float,
    val color: Color,
)

@Composable
fun EnterpriseDonutChart(
    title: String,
    segments: List<DonutSegment>,
    modifier: Modifier = Modifier,
    chartDiameter: Dp = 180.dp,
) {
    if (segments.isEmpty()) return
    val total = segments.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f)
    val animatedSweep by
        animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween(900),
            label = "donut",
        )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Text(title, style = AppTypography.CardTitle, color = MaterialTheme.colorScheme.onSurface)
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(chartDiameter),
        ) {
            val stroke = 28.dp.toPx()
            val r = (minOf(size.width, size.height) - stroke) / 2f
            val c = Offset(size.width / 2f, size.height / 2f)
            var start = -90f
            segments.forEach { seg ->
                val sweep = (seg.value / total) * 360f * animatedSweep
                drawArc(
                    color = seg.color,
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
}

data class LinePoint(
    val xLabel: String,
    val y: Float,
)

@Composable
fun EnterpriseLineChart(
    title: String,
    points: List<LinePoint>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 160.dp,
    lineColor: Color = MaterialTheme.colorScheme.primary,
) {
    if (points.isEmpty()) return
    val max = points.maxOfOrNull { it.y }?.coerceAtLeast(1f) ?: 1f
    val progress by animateFloatAsState(targetValue = 1f, animationSpec = tween(700), label = "line")
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Text(title, style = AppTypography.CardTitle, color = MaterialTheme.colorScheme.onSurface)
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
                    .padding(vertical = 4.dp),
        ) {
            val pad = 12.dp.toPx()
            val w = size.width - pad * 2
            val h = size.height - pad * 2
            val n = (points.size - 1).coerceAtLeast(1)
            val path = Path()
            points.forEachIndexed { i, p ->
                val xf = pad + (i / n.toFloat()) * w
                val yf = pad + h - (p.y / max) * h * progress
                if (i == 0) path.moveTo(xf, yf) else path.lineTo(xf, yf)
            }
            drawPath(
                path = path,
                brush = Brush.horizontalGradient(listOf(lineColor, lineColor.copy(alpha = 0.5f))),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
            )
            points.forEachIndexed { i, p ->
                val xf = pad + (i / n.toFloat()) * w
                val yf = pad + h - (p.y / max) * h * progress
                drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(xf, yf))
            }
        }
    }
}
