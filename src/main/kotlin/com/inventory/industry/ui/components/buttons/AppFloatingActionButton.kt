package com.inventory.industry.ui.components.buttons

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.inventory.industry.ui.modifiers.smoothClickable
import com.inventory.industry.ui.theme.AppElevations
import com.inventory.industry.ui.theme.AppShapes
import com.inventory.industry.ui.theme.AppSpacing

@Composable
fun AppFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    size: Dp = 56.dp,
    shape: RoundedCornerShape = AppShapes.large,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    gradient: Brush? = null,
    content: @Composable () -> Unit,
) {
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val elevation by
        animateDpAsState(
            targetValue =
                when {
                    !enabled -> AppElevations.none
                    pressed -> AppElevations.cardRest
                    hovered -> AppElevations.fab
                    else -> AppElevations.cardRaised
                },
            animationSpec = tween(180),
            label = "fabElevation",
        )
    val scale by
        animateFloatAsState(
            targetValue =
                when {
                    !enabled -> 1f
                    hovered -> 1.06f
                    else -> 1f
                },
            animationSpec = tween(180),
            label = "fabScale",
        )
    val shadowTint = MaterialTheme.colorScheme.primary.copy(alpha = if (hovered) 0.28f else 0.18f)
    val colorLayer =
        if (gradient != null) {
            Modifier.background(brush = gradient)
        } else {
            Modifier.background(color = containerColor)
        }
    Box(
        modifier =
            modifier
                .scale(scale)
                .size(size)
                .shadow(
                    elevation = elevation,
                    shape = shape,
                    clip = false,
                    ambientColor = shadowTint,
                    spotColor = shadowTint,
                )
                .clip(shape)
                .then(colorLayer)
                .smoothClickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    onClick = onClick,
                )
                .padding(AppSpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}
