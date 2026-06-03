package com.inventory.industry.ui.components.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.inventory.industry.data.DashboardActivityEntry
import com.inventory.industry.data.DashboardActivityKind
import com.inventory.industry.ui.components.buttons.AppOutlinedButton
import com.inventory.industry.ui.components.inputs.AppSearchField
import com.inventory.industry.ui.formatEpochMs
import com.inventory.industry.ui.theme.AppSpacing
import com.inventory.industry.ui.theme.AppTypography

@Composable
fun DashboardActividadesTab(
    activity: List<DashboardActivityEntry>,
    pdfExporting: Boolean,
    onExportPdf: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var kindFilter by remember { mutableStateOf<DashboardActivityKind?>(null) }

    val filtered =
        remember(activity, query, kindFilter) {
            val q = query.trim().lowercase()
            activity.filter { e ->
                val kindOk = kindFilter == null || e.kind == kindFilter
                val textOk =
                    q.isEmpty() ||
                        e.title.lowercase().contains(q) ||
                        e.subtitle.lowercase().contains(q)
                kindOk && textOk
            }
        }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Buscar actividad…",
                modifier = Modifier.weight(1f),
            )
            AppOutlinedButton(
                text = if (pdfExporting) "PDF…" else "Exportar PDF",
                onClick = onExportPdf,
                enabled = !pdfExporting,
                leadingIcon = {
                    Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                },
            )
        }
        KindFilterRow(selected = kindFilter, onSelect = { kindFilter = it })
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        ) {
            if (filtered.isEmpty()) {
                item {
                    Text(
                        "Sin resultados",
                        style = AppTypography.BodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(AppSpacing.md),
                    )
                }
            } else {
                items(filtered, key = { "${it.kind}-${it.epochMs}-${it.title}" }) { entry ->
                    ActivityTimelineRow(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun KindFilterRow(
    selected: DashboardActivityKind?,
    onSelect: (DashboardActivityKind?) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        FilterChip("Todas", selected == null) { onSelect(null) }
        FilterChip("Ventas", selected == DashboardActivityKind.Sale) { onSelect(DashboardActivityKind.Sale) }
        FilterChip("Producción", selected == DashboardActivityKind.Transformation) {
            onSelect(DashboardActivityKind.Transformation)
        }
        FilterChip("Traslados", selected == DashboardActivityKind.Transport) {
            onSelect(DashboardActivityKind.Transport)
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val bg =
        if (active) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        }
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(bg)
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            style = AppTypography.Caption,
            color =
                if (active) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
    }
}

@Composable
private fun ActivityTimelineRow(entry: DashboardActivityEntry) {
    val (icon, tint) =
        when (entry.kind) {
            DashboardActivityKind.Sale -> Icons.Outlined.ShoppingCart to Color(0xFF2E7D52)
            DashboardActivityKind.Transformation -> Icons.Default.ShowChart to MaterialTheme.colorScheme.primary
            DashboardActivityKind.Transport -> Icons.Default.LocalShipping to Color(0xFF1565C0)
            DashboardActivityKind.Inventory -> Icons.Outlined.History to MaterialTheme.colorScheme.tertiary
        }
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(entry.title, style = AppTypography.BodySmall)
                Text(
                    entry.subtitle,
                    style = AppTypography.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                formatEpochMs(entry.epochMs),
                style = AppTypography.Caption,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}
