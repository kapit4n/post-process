package com.inventory.industry.ui.components.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inventory.industry.ui.components.feedback.StatusChip
import com.inventory.industry.ui.models.StatusKind
import com.inventory.industry.ui.theme.AppElevations
import com.inventory.industry.ui.theme.AppShapes
import com.inventory.industry.ui.theme.AppSpacing
import com.inventory.industry.ui.theme.AppThemeState
import com.inventory.industry.ui.theme.AppTypography

/**
 * Tarjeta de indicador (KPI) con icono, valor grande, subtítulo, gradiente suave y hover.
 * Reutilizada por los dashboards de Traslados y Ventas.
 */
@Composable
fun KpiCard(
    icon: ImageVector,
    accent: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = AppShapes.medium
    val cardBg = AppThemeState.semantic.cardBackground
    val elevation by animateDpAsState(
        targetValue = if (hovered) AppElevations.cardHovered else AppElevations.cardRest,
        animationSpec = tween(180),
        label = "kpiElevation",
    )
    Box(
        modifier =
            modifier
                .heightIn(min = 120.dp)
                .shadow(
                    elevation = elevation,
                    shape = shape,
                    clip = false,
                    ambientColor = accent.copy(alpha = 0.16f),
                    spotColor = accent.copy(alpha = 0.20f),
                )
                .clip(shape)
                .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.16f), cardBg)))
                .border(1.dp, accent.copy(alpha = 0.22f), shape)
                .hoverable(interaction)
                .padding(AppSpacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape).background(accent.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                }
                Text(
                    label,
                    style = AppTypography.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                value,
                style = AppTypography.MetricMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.let {
                Text(it, style = AppTypography.BodySmall, color = subtitleColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Stepper horizontal reutilizable para flujos tipo asistente. */
@Composable
fun WizardStepper(
    steps: List<String>,
    currentStep: Int,
    onStepClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        steps.forEachIndexed { index, label ->
            val completed = index < currentStep
            val active = index == currentStep
            val primary = MaterialTheme.colorScheme.primary
            val circleColor by animateColorAsState(
                targetValue = when {
                    active -> primary
                    completed -> primary.copy(alpha = 0.85f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                animationSpec = tween(180),
                label = "stepCircle",
            )
            val labelColor =
                if (active || completed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                modifier = Modifier.clickable { onStepClick(index) }.padding(vertical = AppSpacing.xs),
            ) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(circleColor),
                    contentAlignment = Alignment.Center,
                ) {
                    if (completed) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                    } else {
                        Text(
                            "${index + 1}",
                            style = AppTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                            color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    label,
                    style = AppTypography.BodySmall.copy(fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal),
                    color = labelColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (index < steps.lastIndex) {
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(horizontal = AppSpacing.sm)
                            .height(2.dp)
                            .clip(AppShapes.small)
                            .background(
                                if (completed) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                            ),
                )
            }
        }
    }
}

/** Tarjeta seleccionable con borde/glow animado y hover. El contenido va en un Column. */
@Composable
fun SelectableCard(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = AppShapes.medium
    val primary = MaterialTheme.colorScheme.primary
    val cardBg = AppThemeState.semantic.cardBackground
    val borderColor by animateColorAsState(
        targetValue = when {
            selected -> primary
            hovered -> primary.copy(alpha = 0.45f)
            else -> AppThemeState.semantic.border.copy(alpha = 0.5f)
        },
        animationSpec = tween(160),
        label = "selectableBorder",
    )
    val elevation by animateDpAsState(
        targetValue = if (selected || hovered) AppElevations.cardRaised else AppElevations.cardRest,
        animationSpec = tween(160),
        label = "selectableElevation",
    )
    val background = if (selected) primary.copy(alpha = 0.10f) else cardBg
    Box(
        modifier =
            modifier
                .shadow(
                    elevation = elevation,
                    shape = shape,
                    clip = false,
                    ambientColor = primary.copy(alpha = 0.10f),
                    spotColor = primary.copy(alpha = if (selected) 0.25f else 0.12f),
                )
                .clip(shape)
                .background(background)
                .border(width = if (selected) 2.dp else 1.dp, color = borderColor, shape = shape)
                .hoverable(interaction)
                .clickable(interactionSource = interaction, indication = null) { onClick() }
                .padding(AppSpacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) { content() }
    }
}

/** Fila etiqueta → valor para paneles de resumen. */
@Composable
fun SummaryLine(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = AppTypography.BodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = AppTypography.BodySmall.copy(fontWeight = FontWeight.Medium),
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Banner informativo / de error en línea. */
@Composable
fun InlineBanner(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.small)
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), AppShapes.small)
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
    ) {
        Text(message, style = AppTypography.BodySmall, color = color)
    }
}

/** Campo de formulario con icono a la izquierda dentro de un chip cuadrado. */
@Composable
fun FieldWithIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    alignTop: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = if (alignTop) Alignment.Top else Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        Box(
            modifier = Modifier
                .padding(top = if (alignTop) 24.dp else 12.dp)
                .size(36.dp)
                .clip(AppShapes.small)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        content()
    }
}

/** Color asociado a un [StatusKind] (para puntos de timeline, badges, etc.). */
@Composable
fun statusKindColor(kind: StatusKind): Color =
    when (kind) {
        StatusKind.Success -> AppThemeState.semantic.success
        StatusKind.Warning -> AppThemeState.semantic.warning
        StatusKind.Error -> MaterialTheme.colorScheme.error
        StatusKind.Info -> AppThemeState.semantic.info
        StatusKind.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    }

/** Badge tipo píldora con punto de color + texto. */
@Composable
fun StatusDotBadge(
    text: String,
    kind: StatusKind,
    modifier: Modifier = Modifier,
) {
    StatusChip(
        text = text,
        kind = kind,
        modifier = modifier,
        leading = {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusKindColor(kind)))
        },
    )
}

/** Etiqueta de comparación mes actual vs mes anterior (▲/▼/sin cambios). */
fun monthDeltaLabel(current: Int, previous: Int): String {
    val delta = current - previous
    return when {
        delta > 0 -> "▲ $delta vs mes anterior"
        delta < 0 -> "▼ ${-delta} vs mes anterior"
        else -> "Sin cambios vs mes anterior"
    }
}

/** Color para la etiqueta de comparación mensual (sube/baja/neutral). */
fun monthDeltaColor(
    current: Int,
    previous: Int,
    up: Color,
    down: Color,
    neutral: Color,
): Color {
    val delta = current - previous
    return when {
        delta > 0 -> up
        delta < 0 -> down
        else -> neutral
    }
}
