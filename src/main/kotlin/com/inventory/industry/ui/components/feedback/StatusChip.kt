package com.inventory.industry.ui.components.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.inventory.industry.ui.models.StatusKind
import com.inventory.industry.ui.theme.AppShapes
import com.inventory.industry.ui.theme.AppSpacing
import com.inventory.industry.ui.theme.AppThemeState
import com.inventory.industry.ui.theme.AppTypography

@Composable
fun StatusChip(
    text: String,
    kind: StatusKind,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    val (background, foreground) = colorsFor(kind)
    Row(
        modifier =
            modifier
                .background(background, shape = AppShapes.small)
                .padding(horizontal = AppSpacing.sm, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        leading?.invoke()
        Text(text = text, style = AppTypography.Caption, color = foreground)
    }
}

@Composable
private fun colorsFor(kind: StatusKind): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    val semantic = AppThemeState.semantic
    return when (kind) {
        StatusKind.Neutral -> scheme.surfaceVariant to scheme.onSurfaceVariant
        StatusKind.Success -> semantic.success.copy(alpha = 0.14f) to semantic.success
        StatusKind.Warning -> semantic.warning.copy(alpha = 0.16f) to semantic.warning
        StatusKind.Error -> scheme.errorContainer to scheme.error
        StatusKind.Info -> semantic.info.copy(alpha = 0.14f) to semantic.info
    }
}
