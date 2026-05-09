package com.inventory.industry.ui.modifiers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.inventory.industry.ui.theme.AppThemeState

fun Modifier.smoothClickable(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    onClick: () -> Unit,
): Modifier =
    composed(
        inspectorInfo =
            debugInspectorInfo {
                name = "smoothClickable"
                properties["enabled"] = enabled
            },
    ) {
        val source = interactionSource ?: remember { MutableInteractionSource() }
        val resolvedIndication = indication ?: LocalIndication.current
        this.then(
            Modifier.clickable(
                interactionSource = source,
                indication = resolvedIndication,
                enabled = enabled,
                onClick = onClick,
            ),
        )
    }

@Composable
fun Modifier.animatedBorder(
    active: Boolean,
    shape: Shape,
    idleWidth: Dp = 1.dp,
    activeWidth: Dp = 2.dp,
): Modifier {
    val borderColorIdle = AppThemeState.semantic.border
    val borderColorActive = MaterialTheme.colorScheme.primary
    val width by
        animateDpAsState(
            targetValue = if (active) activeWidth else idleWidth,
            animationSpec = tween(160),
            label = "borderWidth",
        )
    val color by
        animateColorAsState(
            targetValue = if (active) borderColorActive else borderColorIdle,
            animationSpec = tween(160),
            label = "borderColor",
        )
    return this.border(width, color, shape)
}

@Composable
fun Modifier.scaleOnHover(
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    defaultScale: Float = 1f,
    hoveredScale: Float = 1.02f,
): Modifier {
    val hovered by interactionSource.collectIsHoveredAsState()
    val scale by
        animateFloatAsState(
            targetValue = if (hovered) hoveredScale else defaultScale,
            animationSpec = tween(160),
            label = "hoverScale",
        )
    return this
        .trackHover(interactionSource)
        .scale(scale)
}

@Composable
fun Modifier.fadeOnCondition(
    visible: Boolean,
    minAlpha: Float = 0f,
    maxAlpha: Float = 1f,
): Modifier {
    val alpha by
        animateFloatAsState(
            targetValue = if (visible) maxAlpha else minAlpha,
            animationSpec = tween(200),
            label = "fadeAlpha",
        )
    return this.alpha(alpha)
}

@Composable
fun AppFadeVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(180)),
        content = content,
    )
}
