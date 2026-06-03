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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inventory.industry.data.InventoryFlowSummary
import com.inventory.industry.ui.components.cards.SectionCard
import com.inventory.industry.ui.formatMoney
import com.inventory.industry.ui.formatQty
import com.inventory.industry.ui.theme.AppSpacing
import com.inventory.industry.ui.theme.AppTypography

@Composable
fun DashboardInventarioTab(
    flow: InventoryFlowSummary,
    totalTransformCost: Double,
    inventoryValue: Double,
    totalLots: Int,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
            ) {
                SectionCard(
                    modifier = Modifier.weight(1f),
                    title = "Valor inventario",
                    subtitle = "Costo adquisición estimado",
                ) {
                    Text(
                        formatMoney(inventoryValue),
                        style = AppTypography.MetricMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                SectionCard(
                    modifier = Modifier.weight(1f),
                    title = "Costo transformación",
                    subtitle = "Insumos acumulados",
                ) {
                    Text(
                        formatMoney(totalTransformCost),
                        style = AppTypography.MetricMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        item {
            SectionCard(
                title = "Resumen de inventario",
                subtitle = "$totalLots lote(s) registrados",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    SummaryLine("En proceso (OK)", formatQty(flow.polesInProcessOk))
                    SummaryLine("Listos para venta", formatQty(flow.polesReadyStandardSale))
                    SummaryLine("Fallados / saldo", formatQty(flow.polesFailedSalvage))
                    HorizontalDivider()
                    Text("Lotes por etapa", style = AppTypography.CardTitle)
                    flow.perStage.forEach { r ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "${r.stage.shortCode} — ${r.lotCount} lote(s)",
                                style = AppTypography.BodySmall,
                            )
                            Text(
                                formatQty(r.totalPoles),
                                style = AppTypography.BodySmall,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
        item {
            SectionCard(title = "Movimiento de stock", subtitle = "Distribución por etapa productiva") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    flow.perStage.forEach { r ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(r.stage.title, style = AppTypography.BodySmall, modifier = Modifier.weight(1f))
                            Text(
                                "OK ${formatQty(r.okPoles)} · Fall. ${formatQty(r.failedPoles)}",
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

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = AppTypography.BodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = AppTypography.BodySmall, fontWeight = FontWeight.SemiBold)
    }
}
