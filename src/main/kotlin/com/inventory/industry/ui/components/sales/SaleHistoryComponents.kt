package com.inventory.industry.ui.components.sales

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inventory.industry.data.SaleRecord
import com.inventory.industry.domain.ProductStage
import com.inventory.industry.ui.formatEpochMs
import com.inventory.industry.ui.formatMoney
import com.inventory.industry.ui.formatQty
import com.inventory.industry.ui.components.cards.AppCard
import com.inventory.industry.ui.components.feedback.StatusChip
import com.inventory.industry.ui.models.StatusKind
import com.inventory.industry.ui.modifiers.smoothClickable
import com.inventory.industry.ui.theme.AppSpacing
import com.inventory.industry.ui.theme.AppThemeState
import com.inventory.industry.ui.theme.AppTypography

enum class SaleLedgerStatus(
    val label: String,
    val kind: StatusKind,
) {
    CompletedOk("Terminado OK", StatusKind.Success),
    Pending("Pendiente", StatusKind.Warning),
    Failed("Fallado", StatusKind.Error),
    Cancelled("Cancelado", StatusKind.Neutral),
}

fun SaleRecord.toLedgerStatus(): SaleLedgerStatus {
    if (notes?.contains("cancel", ignoreCase = true) == true) {
        return SaleLedgerStatus.Cancelled
    }
    return when {
        snapshotWasFailed -> SaleLedgerStatus.Failed
        snapshotStage == ProductStage.TERMINADO -> SaleLedgerStatus.CompletedOk
        else -> SaleLedgerStatus.Pending
    }
}

@Composable
fun SaleStatusChip(
    status: SaleLedgerStatus,
    modifier: Modifier = Modifier,
) {
    StatusChip(text = status.label, kind = status.kind, modifier = modifier)
}

@Composable
fun SaleMetricColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    emphasize: Boolean = false,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
    val textAlign =
        when (horizontalAlignment) {
            Alignment.CenterHorizontally -> TextAlign.Center
            Alignment.End -> TextAlign.End
            else -> TextAlign.Start
        }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = horizontalAlignment,
    ) {
        Text(
            text = label.uppercase(),
            style = AppTypography.Caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = value,
            style = if (emphasize) AppTypography.MetricMedium else AppTypography.Body,
            color = valueColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun SaleHistoryCard(
    sale: SaleRecord,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val status = sale.toLedgerStatus()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val scale by
        animateFloatAsState(
            targetValue = if (hovered) 1.01f else 1f,
            animationSpec = tween(160),
            label = "saleCardScale",
        )
    AppCard(
        modifier =
            modifier
                .fillMaxWidth()
                .scale(scale)
                .smoothClickable(interactionSource = interaction, onClick = onClick),
        contentPadding = PaddingValues(AppSpacing.md),
        enableHoverElevation = true,
        showHairlineBorder = true,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = sale.clientName,
                        style = AppTypography.CardTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = formatEpochMs(sale.soldAtEpochMs),
                        style = AppTypography.BodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SaleStatusChip(status = status)
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp).padding(start = AppSpacing.sm),
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text =
                    "${sale.snapshotProductName} · ${sale.snapshotStage.shortCode} · " +
                        "${formatQty(sale.quantitySold)} postes",
                style = AppTypography.BodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                verticalAlignment = Alignment.Top,
            ) {
                val costParts =
                    buildString {
                        append(formatMoney(sale.snapshotAcquisitionCostTotal))
                        if (sale.snapshotProcessingCostTotal > 1e-9) {
                            append(" · proc. ")
                            append(formatMoney(sale.snapshotProcessingCostTotal))
                        }
                    }
                SaleMetricColumn(
                    label = "Costo imputado",
                    value = costParts,
                    modifier = Modifier.weight(1f),
                    valueColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    horizontalAlignment = Alignment.Start,
                )
                val sug = sale.snapshotSuggestedTotal
                if (sug != null) {
                    SaleMetricColumn(
                        label = "Sugerido al vender",
                        value = formatMoney(sug),
                        modifier = Modifier.weight(1f),
                        valueColor = AppThemeState.semantic.info,
                        horizontalAlignment = Alignment.End,
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
