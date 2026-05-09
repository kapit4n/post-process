package com.inventory.industry.ui.components.buttons

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.inventory.industry.ui.modifiers.smoothClickable
import com.inventory.industry.ui.theme.AppElevations
import com.inventory.industry.ui.theme.AppShapes
import com.inventory.industry.ui.theme.AppSpacing
import com.inventory.industry.ui.theme.AppThemeState
import com.inventory.industry.ui.theme.AppTypography

private val OutlinedButtonMinHeight = 44.dp
private const val OutlinedMotionMs = 180

@Composable
fun AppOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    contentPadding: PaddingValues =
        PaddingValues(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
    shape: RoundedCornerShape = AppShapes.medium,
    borderBrush: Brush? = null,
    borderColor: Color = AppThemeState.semantic.border,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = Color.Transparent,
    minHeight: Dp = OutlinedButtonMinHeight,
) {
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val elevation by
        animateDpAsState(
            targetValue =
                when {
                    !enabled -> AppElevations.none
                    pressed -> AppElevations.hairline
                    hovered -> AppElevations.low
                    else -> AppElevations.none
                },
            animationSpec = tween(OutlinedMotionMs),
            label = "outlinedButtonElevation",
        )
    val borderMod =
        if (borderBrush != null) {
            Modifier.border(width = 1.dp, brush = borderBrush, shape = shape)
        } else {
            Modifier.border(width = 1.dp, color = borderColor, shape = shape)
        }
    val shadowTint = MaterialTheme.colorScheme.primary.copy(alpha = if (hovered) 0.12f else 0.06f)
    Box(
        modifier =
            modifier
                .heightIn(min = minHeight)
                .shadow(
                    elevation = elevation,
                    shape = shape,
                    clip = false,
                    ambientColor = shadowTint,
                    spotColor = shadowTint,
                )
                .clip(shape)
                .then(borderMod)
                .background(color = backgroundColor)
                .smoothClickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    onClick = onClick,
                )
                .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingIcon?.invoke()
            Text(
                text = text,
                style = AppTypography.ButtonText,
                color = if (enabled) contentColor else contentColor.copy(alpha = 0.38f),
            )
            trailingIcon?.invoke()
        }
    }
}
