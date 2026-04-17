package com.inventory.industry.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inventory.industry.data.InventoryFlowSummary
import com.inventory.industry.data.InventoryRepository
import com.inventory.industry.domain.ProductStage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DashboardScreen(repo: InventoryRepository) {
    var counts by remember { mutableStateOf<Map<ProductStage, Int>?>(null) }
    var polesByStage by remember { mutableStateOf<Map<ProductStage, Double>?>(null) }
    var flow by remember { mutableStateOf<InventoryFlowSummary?>(null) }
    var total by remember { mutableStateOf<Double?>(null) }
    var productCount by remember { mutableStateOf<Int?>(null) }
    var failedCount by remember { mutableStateOf<Int?>(null) }
    var failedValue by remember { mutableStateOf<Double?>(null) }
    var catalogCount by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            counts = repo.countByStage()
            polesByStage = repo.polesByStage()
            flow = repo.inventoryFlowSummary()
            total = repo.totalProcessCost()
            productCount = repo.listProducts().size
            failedCount = repo.failedProductCount()
            failedValue = repo.failedStockSaleValueEstimate()
            catalogCount = repo.listCatalogProducts().size
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.verticalScroll(rememberScrollState()),
    ) {
        Text(
            "Resumen",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Los postes de madera avanzan desde el ingreso en Crudo, " +
                "pasan por Descortezado y Secado, luego Tratado Químicamente, " +
                "hasta quedar Terminados y listos para la venta. " +
                "Cada avance registra los insumos usados y su costo.",
            style = MaterialTheme.typography.bodyMedium,
        )

        if (counts == null || polesByStage == null || flow == null || total == null || productCount == null ||
            failedCount == null || failedValue == null || catalogCount == null
        ) {
            Text("Cargando…")
            return@Column
        }

        val f = flow!!

        Text("Flujo de inventario", style = MaterialTheme.typography.titleMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricRow(
                    "Postes OK en proceso (Crudo / Descort. / Tratado)",
                    formatQty(f.polesInProcessOk),
                )
                MetricRow(
                    "Listos para venta estándar (Terminado, OK)",
                    formatQty(f.polesReadyStandardSale),
                )
                MetricRow(
                    "Stock fallado / saldo (vendible a otro precio)",
                    formatQty(f.polesFailedSalvage),
                )
            }
        }

        Text("Detalle por etapa (postes OK vs fallados)", style = MaterialTheme.typography.titleMedium)
        f.perStage.forEach { row ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${row.stage.shortCode} — ${row.stage.title}",
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Total: ${formatQty(row.totalPoles)} postes · ${row.lotCount} lote(s)",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "OK (pueden seguir o venderse según etapa): ${formatQty(row.okPoles)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "Fallados en esta etapa: ${formatQty(row.failedPoles)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        MetricRow("Definiciones en catálogo", catalogCount!!.toString())
        MetricRow("Lotes registrados (total)", productCount!!.toString())
        MetricRow("Lotes marcados como fallados", failedCount!!.toString())
        MetricRow(
            "Stock fallado a precio de saldo (cant. × precio)",
            formatMoney(failedValue!!),
        )
        MetricRow("Gasto en insumos de transformación", formatMoney(total!!))

        Text("Postes por etapa", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProductStage.entries.forEach { s ->
                val poles = polesByStage!![s] ?: 0.0
                val lots = counts!![s] ?: 0
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = s.shortCode,
                    subtitle = "${s.title} · $lots lote(s)",
                    value = formatQty(poles),
                )
            }
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MetricCard(
    title: String,
    subtitle: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}
