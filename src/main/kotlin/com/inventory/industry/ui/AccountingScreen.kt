package com.inventory.industry.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.inventory.industry.data.AccountingBucket
import com.inventory.industry.data.AccountingCostOverview
import com.inventory.industry.data.InventoryRepository
import com.inventory.industry.ui.components.cards.AppCard
import com.inventory.industry.ui.components.cards.SectionCard
import com.inventory.industry.ui.components.inputs.AppTextField
import com.inventory.industry.ui.layout.EnterpriseScreenLayout
import com.inventory.industry.ui.theme.AppSpacing
import com.inventory.industry.ui.theme.AppTypography
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    EnterpriseScreenLayout(
        title = "Contabilidad de ventas",
        subtitle = "Totales por día, mes o año según la fecha registrada en cada venta.",
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
            costOverview?.let { o ->
                SectionCard(title = "Costos", subtitle = "Inventario, proceso y traslados") {
                    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                        Text(
                            "Procesamiento acumulado (histórico): ${formatMoney(o.totalProcessingCostAllTime)}",
                            style = AppTypography.BodySmall,
                        )
                        Text(
                            "Procesamiento imputado a stock abierto: ${formatMoney(o.processingCostAttributedToOpenStock)}",
                            style = AppTypography.BodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Adquisición en inventario: ${formatMoney(o.inventoryAcquisitionCostTotal)}",
                            style = AppTypography.BodySmall,
                        )
                        Text(
                            "Traslado proveedor histórico: ${formatMoney(o.totalAcquisitionTransportAllTime)} " +
                                "· en stock: ${formatMoney(o.acquisitionTransportAttributedToOpenStock)}",
                            style = AppTypography.BodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        HorizontalDivider()
                        Text(
                            "Ventas registradas — adquisición: ${formatMoney(o.soldAcquisitionCostTotal)} " +
                                "(traslado ${formatMoney(o.soldAcquisitionTransportTotal)}) · proceso: " +
                                formatMoney(o.soldProcessingCostTotal),
                            style = AppTypography.BodySmall,
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
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                    AppTextField(
                        value = yearText,
                        onValueChange = { yearText = it },
                        label = "Año",
                        modifier = Modifier.weight(1f),
                    )
                    if (tab == 0) {
                        AppTextField(
                            value = monthText,
                            onValueChange = { monthText = it },
                            label = "Mes (1-12)",
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
                Text("Total período", style = AppTypography.SectionTitle)
                Text(formatMoney(sumMoney), style = AppTypography.SectionTitle, fontWeight = FontWeight.Bold)
            }
            Text(
                "${formatQty(sumPoles)} postes · $sumTx operaciones",
                style = AppTypography.BodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                modifier = Modifier.weight(1f),
            ) {
                items(rows, key = { it.periodKey }) { b ->
                    AppCard(enableHoverElevation = false) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(b.displayLabel, style = AppTypography.CardTitle)
                                Text(
                                    "${b.saleCount} ventas · ${formatQty(b.totalPolesSold)} postes",
                                    style = AppTypography.Caption,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(formatMoney(b.totalAmount), style = AppTypography.Body, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
