package com.inventory.industry.ui.components.buttons

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.inventory.industry.ui.modifiers.smoothClickable
import com.inventory.industry.ui.theme.AppElevations
@Composable
fun AppIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    size: Dp = 44.dp,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    hoveredColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    content: @Composable () -> Unit,
) {
    val hovered by interactionSource.collectIsHoveredAsState()
    val elevation by
        animateDpAsState(
            targetValue = if (hovered && enabled) AppElevations.low else AppElevations.none,
            animationSpec = tween(160),
            label = "iconButtonElevation",
        )
    val tint = MaterialTheme.colorScheme.primary.copy(alpha = if (hovered) 0.18f else 0.1f)
    Box(
        modifier =
            modifier
                .size(size)
                .shadow(
                    elevation = elevation,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = tint,
                    spotColor = tint,
                )
                .clip(CircleShape)
                .background(if (hovered && enabled) hoveredColor else containerColor)
                .smoothClickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
