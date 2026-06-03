package com.inventory.industry.ui.components.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inventory.industry.ui.components.cards.AppCard
import com.inventory.industry.ui.theme.AppShapes
import com.inventory.industry.ui.theme.AppSpacing
import com.inventory.industry.ui.theme.AppTypography

@Composable
fun CompactKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accent: Brush,
    modifier: Modifier = Modifier,
) {
    AppCard(
        modifier = modifier.heightIn(max = 110.dp).height(110.dp),
        enableHoverElevation = true,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(AppShapes.medium)
                        .background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    title,
                    style = AppTypography.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    value,
                    style = AppTypography.MetricMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    subtitle,
                    style = AppTypography.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun CompactKpiRow(
    enProceso: String,
    listos: String,
    fallados: String,
    lotes: String,
    valorInventario: String,
    modifier: Modifier = Modifier,
    enProcesoBrush: Brush,
    listosBrush: Brush,
    falladosBrush: Brush,
    lotesBrush: Brush,
    valorBrush: Brush,
    enProcesoIcon: ImageVector,
    listosIcon: ImageVector,
    falladosIcon: ImageVector,
    lotesIcon: ImageVector,
    valorIcon: ImageVector,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        CompactKpiCard(
            title = "En proceso",
            value = enProceso,
            subtitle = "Crudo · Desc. · Tratado",
            icon = enProcesoIcon,
            accent = enProcesoBrush,
            modifier = Modifier.weight(1f),
        )
        CompactKpiCard(
            title = "Listos",
            value = listos,
            subtitle = "Terminado OK",
            icon = listosIcon,
            accent = listosBrush,
            modifier = Modifier.weight(1f),
        )
        CompactKpiCard(
            title = "Fallados",
            value = fallados,
            subtitle = "Saldo / falla",
            icon = falladosIcon,
            accent = falladosBrush,
            modifier = Modifier.weight(1f),
        )
        CompactKpiCard(
            title = "Lotes",
            value = lotes,
            subtitle = "Registrados",
            icon = lotesIcon,
            accent = lotesBrush,
            modifier = Modifier.weight(1f),
        )
        CompactKpiCard(
            title = "Valor inventario",
            value = valorInventario,
            subtitle = "Costo adquisición est.",
            icon = valorIcon,
            accent = valorBrush,
            modifier = Modifier.weight(1f),
        )
    }
}
