package com.inventory.industry.ui.components.sales

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inventory.industry.reports.SalesReportRange
import com.inventory.industry.ui.components.inputs.AppTextField
import com.inventory.industry.ui.formatIsoDate
import com.inventory.industry.ui.parseIsoDate
import com.inventory.industry.ui.theme.AppSpacing
import com.inventory.industry.ui.theme.AppTypography
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SalesReportRangeDialog(
    onDismiss: () -> Unit,
    onGenerate: (SalesReportRange, LocalDate?, LocalDate?) -> Unit,
    exporting: Boolean,
) {
    var selected by remember { mutableStateOf(SalesReportRange.MONTH) }
    var fromText by remember { mutableStateOf(formatIsoDate(LocalDate.now())) }
    var toText by remember { mutableStateOf(formatIsoDate(LocalDate.now())) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exportar reporte de ventas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                Text(
                    "Seleccione el periodo del informe PDF.",
                    style = AppTypography.BodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                ) {
                    SalesReportRange.entries.forEach { range ->
                        FilterChip(
                            selected = selected == range,
                            onClick = { selected = range },
                            label = { Text(range.label, style = AppTypography.Caption) },
                            shape = RoundedCornerShape(8.dp),
                            colors =
                                FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                        )
                    }
                }
                if (selected == SalesReportRange.CUSTOM) {
                    AppTextField(
                        value = fromText,
                        onValueChange = { fromText = it },
                        label = "Desde (yyyy-MM-dd)",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    AppTextField(
                        value = toText,
                        onValueChange = { toText = it },
                        label = "Hasta (yyyy-MM-dd)",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                validationError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = AppTypography.BodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    validationError = null
                    if (selected == SalesReportRange.CUSTOM) {
                        val from = parseIsoDate(fromText)
                        val to = parseIsoDate(toText)
                        if (from == null || to == null) {
                            validationError = "Ingrese fechas validas (yyyy-MM-dd)."
                            return@TextButton
                        }
                        onGenerate(selected, from, to)
                    } else {
                        onGenerate(selected, null, null)
                    }
                },
                enabled = !exporting,
            ) {
                Text(if (exporting) "Generando…" else "Generar PDF")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !exporting) {
                Text("Cancelar")
            }
        },
    )
}
