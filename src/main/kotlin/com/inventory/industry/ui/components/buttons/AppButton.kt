package com.inventory.industry.ui.components.buttons

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import com.inventory.industry.ui.theme.AppTypography

private val AppButtonMinHeight = 44.dp
private const val ButtonMotionMs = 180

@Composable
fun AppButton(
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
    gradient: Brush? = null,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    minHeight: Dp = AppButtonMinHeight,
) {
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val elevation by
        animateDpAsState(
            targetValue =
                when {
                    !enabled -> AppElevations.none
                    pressed -> AppElevations.low
                    hovered -> AppElevations.cardRaised
                    else -> AppElevations.low
                },
            animationSpec = tween(ButtonMotionMs),
            label = "appButtonElevation",
        )
    val shadowTint = containerColor.copy(alpha = if (hovered) 0.22f else 0.14f)
    val clipMod = Modifier.clip(shape)
    val colorMod =
        if (gradient != null) {
            Modifier.background(brush = gradient)
        } else {
            Modifier.background(color = containerColor)
        }
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
                .then(clipMod)
                .then(colorMod)
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
