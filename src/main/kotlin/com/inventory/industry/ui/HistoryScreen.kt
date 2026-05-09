package com.inventory.industry.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.inventory.industry.ui.layout.EnterpriseScreenLayout
import com.inventory.industry.ui.theme.AppSpacing
import com.inventory.industry.ui.theme.AppTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HistoryScreen(repo: InventoryRepository) {
    var rows by remember { mutableStateOf<List<Transformation>?>(null) }

    LaunchedEffect(Unit) {
        rows = withContext(Dispatchers.IO) { repo.listTransformations() }
    }

    EnterpriseScreenLayout(
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
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                        items(data, key = { it.id }) { t ->
                            TransformationCard(t)
                        }
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
