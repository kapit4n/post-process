package com.inventory.industry.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.inventory.industry.ui.modifiers.smoothClickable
import com.inventory.industry.ui.theme.AppSpacing
import com.inventory.industry.ui.theme.AppTypography

private val PillShape = RoundedCornerShape(percent = 50)

@Composable
fun SidebarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    collapsed: Boolean,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    iconSize: Dp = 22.dp,
) {
    val hovered by interactionSource.collectIsHoveredAsState()
    val scale by
        animateFloatAsState(
            targetValue = if (hovered) 1.04f else 1f,
            animationSpec = tween(160),
            label = "sidebarItemScale",
        )
    val pillColor by
        animateColorAsState(
            targetValue =
                when {
                    selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    hovered -> MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
                    else -> Color.Transparent
                },
            animationSpec = tween(160),
            label = "sidebarPill",
        )
    val contentColor by
        animateColorAsState(
            targetValue =
                when {
                    selected -> MaterialTheme.colorScheme.primary
                    hovered -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            animationSpec = tween(140),
            label = "sidebarContent",
        )
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(PillShape)
                .background(pillColor)
                .smoothClickable(interactionSource = interactionSource, onClick = onClick)
                .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.sm)
                .scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(iconSize),
            tint = contentColor,
        )
        AnimatedVisibility(
            visible = !collapsed,
            enter = fadeIn(tween(140)),
            exit = fadeOut(tween(100)),
        ) {
            Text(
                text = label,
                style = AppTypography.Caption,
                color = contentColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
