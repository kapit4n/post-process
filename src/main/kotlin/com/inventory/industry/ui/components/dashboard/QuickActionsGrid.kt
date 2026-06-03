package com.inventory.industry.ui.components.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inventory.industry.ui.components.buttons.AppButton
import com.inventory.industry.ui.components.buttons.AppOutlinedButton
import com.inventory.industry.ui.components.cards.SectionCard
import com.inventory.industry.ui.navigation.ScreenRoute
import com.inventory.industry.ui.theme.AppSpacing

@Composable
fun QuickActionsGrid(
    onNavigate: (ScreenRoute) -> Unit,
    pdfExporting: Boolean,
    onExportPdf: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(
        modifier = modifier.heightIn(max = 240.dp),
        title = "Acciones rápidas",
        subtitle = "Atajos operativos",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                AppButton(
                    text = "Nuevo lote",
                    onClick = { onNavigate(ScreenRoute.ByStage) },
                    modifier = Modifier.weight(1f),
                )
                AppOutlinedButton(
                    text = "Registrar venta",
                    onClick = { onNavigate(ScreenRoute.Sales) },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                AppOutlinedButton(
                    text = "Crear receta",
                    onClick = { onNavigate(ScreenRoute.Recipes) },
                    modifier = Modifier.weight(1f),
                )
                AppOutlinedButton(
                    text = "Proveedor",
                    onClick = { onNavigate(ScreenRoute.Providers) },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                AppOutlinedButton(
                    text = "Catálogo",
                    onClick = { onNavigate(ScreenRoute.Catalog) },
                    modifier = Modifier.weight(1f),
                )
                AppOutlinedButton(
                    text = if (pdfExporting) "Generando PDF…" else "Exportar PDF",
                    onClick = onExportPdf,
                    enabled = !pdfExporting,
                    modifier = Modifier.weight(1f),
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
            }
        }
    }
}
