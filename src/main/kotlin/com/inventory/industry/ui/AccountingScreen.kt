package com.inventory.industry.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inventory.industry.data.AccountingBucket
import com.inventory.industry.data.AccountingCostOverview
import com.inventory.industry.data.InventoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun AccountingScreen(repo: InventoryRepository) {
    var tab by remember { mutableStateOf(0) }
    val today = LocalDate.now()
    var yearText by remember { mutableStateOf(today.year.toString()) }
    var monthText by remember { mutableStateOf(today.monthValue.toString()) }
    var dailyRows by remember { mutableStateOf<List<AccountingBucket>>(emptyList()) }
    var monthlyRows by remember { mutableStateOf<List<AccountingBucket>>(emptyList()) }
    var yearlyRows by remember { mutableStateOf<List<AccountingBucket>>(emptyList()) }
    var costOverview by remember { mutableStateOf<AccountingCostOverview?>(null) }

    val year = yearText.toIntOrNull() ?: today.year
    val month = monthText.toIntOrNull()?.coerceIn(1, 12) ?: today.monthValue

    LaunchedEffect(Unit) {
        costOverview =
            withContext(Dispatchers.IO) {
                repo.accountingCostOverview()
            }
    }

    LaunchedEffect(tab, year, month) {
        when (tab) {
            0 ->
                dailyRows =
                    withContext(Dispatchers.IO) {
                        repo.salesAggregatedDaily(year, month)
                    }
            1 ->
                monthlyRows =
                    withContext(Dispatchers.IO) {
                        repo.salesAggregatedMonthly(year)
                    }
            2 ->
                yearlyRows =
                    withContext(Dispatchers.IO) {
                        repo.salesAggregatedYearly()
                    }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Contabilidad de ventas",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Totales por día, mes o año según la fecha registrada en cada venta.",
            style = MaterialTheme.typography.bodyMedium,
        )
        costOverview?.let { o ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Costos (inventario y proceso)", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Procesamiento acumulado (histórico, todas las líneas de insumo): " +
                            formatMoney(o.totalProcessingCostAllTime),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "Procesamiento aún imputado a lotes en stock: " +
                            formatMoney(o.processingCostAttributedToOpenStock),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Valor de adquisición en inventario (material + traslado prorrateado por poste): " +
                            formatMoney(o.inventoryAcquisitionCostTotal),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "Traslado desde proveedor (histórico, todas las líneas): " +
                            formatMoney(o.totalAcquisitionTransportAllTime) +
                            " · aún en stock: " +
                            formatMoney(o.acquisitionTransportAttributedToOpenStock),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDivider()
                    Text(
                        "En ventas ya registradas — adquisición imputada: " +
                            formatMoney(o.soldAcquisitionCostTotal) +
                            " (traslado " + formatMoney(o.soldAcquisitionTransportTotal) + ")" +
                            " · proceso imputado: " +
                            formatMoney(o.soldProcessingCostTotal),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Diario") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Mensual") })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Anual") })
        }
        if (tab != 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = yearText,
                    onValueChange = { yearText = it },
                    label = { Text("Año") },
                    modifier = Modifier.weight(1f),
                )
                if (tab == 0) {
                    TextField(
                        value = monthText,
                        onValueChange = { monthText = it },
                        label = { Text("Mes (1-12)") },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        val rows =
            when (tab) {
                0 -> dailyRows
                1 -> monthlyRows
                else -> yearlyRows
            }
        val sumMoney = rows.sumOf { it.totalAmount }
        val sumPoles = rows.sumOf { it.totalPolesSold }
        val sumTx = rows.sumOf { it.saleCount }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Total período", fontWeight = FontWeight.SemiBold)
            Text(formatMoney(sumMoney), fontWeight = FontWeight.Bold)
        }
        Text(
            "${formatQty(sumPoles)} postes · $sumTx operaciones",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider()
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(rows, key = { it.periodKey }) { b ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(b.displayLabel, fontWeight = FontWeight.Medium)
                            Text(
                                "${b.saleCount} ventas · ${formatQty(b.totalPolesSold)} postes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(formatMoney(b.totalAmount), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
