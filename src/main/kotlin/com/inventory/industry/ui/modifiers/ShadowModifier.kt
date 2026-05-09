package com.inventory.industry.ui.modifiers

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.inventory.industry.ui.theme.AppElevations
import com.inventory.industry.ui.theme.AppShapes

@Composable
fun Modifier.cardShadow(
    elevation: Dp = AppElevations.cardRest,
    shape: Shape = AppShapes.medium,
    clip: Boolean = false,
): Modifier {
    val ambient = MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.08f)
    val spot = MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.12f)
    return this.shadow(
        elevation = elevation,
        shape = shape,
        clip = clip,
        ambientColor = ambient,
        spotColor = spot,
    )
}

@Composable
fun Modifier.hoverableCard(
    shape: Shape = AppShapes.medium,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    defaultElevation: Dp = AppElevations.cardRest,
    hoveredElevation: Dp = AppElevations.cardHovered,
    clip: Boolean = false,
): Modifier {
    val hovered by interactionSource.collectIsHoveredAsState()
    val elevation by
        animateDpAsState(
            targetValue = if (hovered) hoveredElevation else defaultElevation,
            animationSpec = tween(durationMillis = 180),
            label = "cardHoverElevation",
        )
    val ambient = MaterialTheme.colorScheme.surfaceTint.copy(alpha = if (hovered) 0.12f else 0.08f)
    val spot = MaterialTheme.colorScheme.surfaceTint.copy(alpha = if (hovered) 0.18f else 0.12f)
    return this
        .trackHover(interactionSource)
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = clip,
            ambientColor = ambient,
            spotColor = spot,
        )
}
