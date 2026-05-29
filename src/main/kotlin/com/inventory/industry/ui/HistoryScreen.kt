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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inventory.industry.data.InventoryRepository
import com.inventory.industry.data.Transformation
import com.inventory.industry.data.TransformationProcessingStatus
import com.inventory.industry.ui.components.feedback.EmptyState
import com.inventory.industry.ui.components.table.ListPaginationFooter
import com.inventory.industry.ui.layout.EnterpriseScreenLayout
import com.inventory.industry.ui.theme.AppSpacing
import com.inventory.industry.ui.theme.AppTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val historyPageSizes = listOf(5, 10, 25, 50)

@Composable
fun HistoryScreen(repo: InventoryRepository) {
    var rows by remember { mutableStateOf<List<Transformation>?>(null) }
    var page by remember { mutableIntStateOf(0) }
    var rowsPerPage by remember { mutableIntStateOf(10) }

    LaunchedEffect(Unit) {
        rows = withContext(Dispatchers.IO) { repo.listTransformations(limit = null) }
    }

    LaunchedEffect(rowsPerPage) {
        page = 0
    }

    EnterpriseScreenLayout(
        modifier = Modifier.fillMaxSize(),
        title = "Historial de transformaciones",
        subtitle =
            "Registro de procesamientos terminados y en curso entre etapas. " +
                "Avance lotes desde «Por etapa».",
    ) {
        when (val data = rows) {
            null -> Text("Cargando…", style = AppTypography.Body)
            else -> {
                if (data.isEmpty()) {
                    EmptyState(
                        title = "Sin transformaciones",
                        message = "Aún no hay transformaciones registradas.",
                    )
                } else {
                    val pageCount =
                        remember(data.size, rowsPerPage) {
                            val n = data.size
                            if (n == 0) 1 else (n + rowsPerPage - 1) / rowsPerPage
                        }
                    LaunchedEffect(page, pageCount) {
                        if (page >= pageCount) {
                            page = (pageCount - 1).coerceAtLeast(0)
                        }
                    }
                    val paged =
                        remember(data, page, rowsPerPage) {
                            data.drop(page * rowsPerPage).take(rowsPerPage)
                        }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                    ) {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                        ) {
                            items(paged, key = { it.id }) { t ->
                                TransformationCard(t)
                            }
                        }
                        ListPaginationFooter(
                            page = page,
                            pageCount = pageCount,
                            rowsPerPage = rowsPerPage,
                            onRowsPerPageChange = { rowsPerPage = it },
                            totalItems = data.size,
                            onPrev = { page = (page - 1).coerceAtLeast(0) },
                            onNext = { page = (page + 1).coerceAtMost(pageCount - 1) },
                            pageSizes = historyPageSizes,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransformationCard(t: Transformation) {
    val inProgress = t.processingStatus == TransformationProcessingStatus.IN_PROGRESS
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "#${t.id} · ${t.fromStage.shortCode} → ${t.toStage.shortCode}" +
                        if (inProgress) " · En curso" else "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color =
                        if (inProgress) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    formatEpochMs(t.startedAtEpochMs ?: t.processedAtEpochMs),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Metric("Entrada", formatQty(t.totalInput))
                Metric(
                    "Exitosos",
                    if (inProgress) "—" else formatQty(t.successCount),
                )
                Metric(
                    "Fallados",
                    if (inProgress) "—" else formatQty(t.failedCount),
                    error = !inProgress && t.failedCount > 0,
                )
                Metric(
                    "Duración",
                    if (inProgress) "—" else formatDuration(t.durationMinutes),
                )
                Metric(
                    "Costo insumos",
                    if (inProgress) "—" else formatMoney(t.totalCost),
                )
            }
            HorizontalDivider()
            Text("Lotes origen", style = MaterialTheme.typography.labelLarge)
            t.inputs.forEach { inp ->
                Text(
                    "· ${inp.sourceName} (${inp.sourceLine}) — ${formatQty(inp.quantity)} postes",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            t.notes?.takeIf { it.isNotBlank() }?.let {
                HorizontalDivider()
                Text("Notas", style = MaterialTheme.typography.labelLarge)
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun Metric(
    label: String,
    value: String,
    error: Boolean = false,
) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color =
                if (error) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface,
        )
    }
}
