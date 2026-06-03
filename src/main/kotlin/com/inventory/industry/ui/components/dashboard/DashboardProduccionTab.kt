package com.inventory.industry.ui.components.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inventory.industry.data.AccountingBucket
import com.inventory.industry.data.InventoryFlowSummary
import com.inventory.industry.data.StageInventoryRow
import com.inventory.industry.ui.charts.BarChartEntry
import com.inventory.industry.ui.charts.EnterpriseBarChart
import com.inventory.industry.ui.charts.EnterpriseLineChart
import com.inventory.industry.ui.charts.LinePoint
import com.inventory.industry.ui.components.cards.SectionCard
import com.inventory.industry.ui.formatQty
import com.inventory.industry.ui.theme.AppSpacing
import com.inventory.industry.ui.theme.AppTypography

@Composable
fun DashboardProduccionTab(
    flow: InventoryFlowSummary,
    salesBuckets: List<AccountingBucket>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        item {
            ProductionMetricsRow(rows = flow.perStage)
        }
        item {
            SectionCard(title = "Producción por etapa", subtitle = "Volumen de postes activos") {
                val barEntries =
                    flow.perStage.mapIndexed { i, r ->
                        val hue =
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                Color(0xFF5C6BC0),
                                Color(0xFF26A69A),
                                Color(0xFFFFB74D),
                            )[i % 4]
                        BarChartEntry(r.stage.shortCode, r.totalPoles.toFloat(), hue)
                    }
                EnterpriseBarChart(title = "", entries = barEntries, chartHeight = 200.dp)
            }
        }
        item {
            val linePoints =
                salesBuckets.mapIndexed { i, b ->
                    LinePoint(i.toString(), b.totalPolesSold.toFloat().coerceAtLeast(0f))
                }
            if (linePoints.isNotEmpty()) {
                SectionCard(title = "Tendencia de ventas", subtitle = "Postes vendidos por día (mes actual)") {
                    EnterpriseLineChart(
                        title = "",
                        points = linePoints,
                        chartHeight = 160.dp,
                        lineColor = Color(0xFF3949AB),
                    )
                }
            }
        }
        item {
            StageProductionTable(rows = flow.perStage)
        }
    }
}

@Composable
private fun ProductionMetricsRow(rows: List<StageInventoryRow>) {
    val totalFailed = rows.sumOf { it.failedPoles }
    val totalOk = rows.sumOf { it.okPoles }
    val totalLots = rows.sumOf { it.lotCount }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        MetricChip("Lotes activos", totalLots.toString(), Modifier.weight(1f))
        MetricChip("Postes OK", formatQty(totalOk), Modifier.weight(1f))
        MetricChip("Postes fallados", formatQty(totalFailed), Modifier.weight(1f))
    }
}

@Composable
private fun MetricChip(label: String, value: String, modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier, title = label, subtitle = null) {
        Text(value, style = AppTypography.MetricMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StageProductionTable(rows: List<StageInventoryRow>) {
    SectionCard(title = "Tabla por etapa", subtitle = "Throughput y fallos") {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                Text("Etapa", style = AppTypography.Caption, modifier = Modifier.weight(1.2f))
                Text("Lotes", style = AppTypography.Caption, modifier = Modifier.weight(0.6f))
                Text("Total", style = AppTypography.Caption, modifier = Modifier.weight(0.7f))
                Text("OK", style = AppTypography.Caption, modifier = Modifier.weight(0.7f))
                Text("Fallados", style = AppTypography.Caption, modifier = Modifier.weight(0.7f))
            }
            HorizontalDivider()
            rows.forEach { r ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(r.stage.shortCode, style = AppTypography.BodySmall, modifier = Modifier.weight(1.2f))
                    Text(r.lotCount.toString(), style = AppTypography.BodySmall, modifier = Modifier.weight(0.6f))
                    Text(formatQty(r.totalPoles), style = AppTypography.BodySmall, modifier = Modifier.weight(0.7f))
                    Text(formatQty(r.okPoles), style = AppTypography.BodySmall, modifier = Modifier.weight(0.7f))
                    Text(
                        formatQty(r.failedPoles),
                        style = AppTypography.BodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(0.7f),
                    )
                }
            }
        }
    }
}
