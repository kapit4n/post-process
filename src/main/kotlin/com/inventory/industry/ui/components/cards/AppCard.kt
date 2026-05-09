package com.inventory.industry.ui.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import com.inventory.industry.ui.modifiers.cardShadow
import com.inventory.industry.ui.modifiers.hoverableCard
import com.inventory.industry.ui.theme.AppShapes
import com.inventory.industry.ui.theme.AppSpacing
import com.inventory.industry.ui.theme.AppThemeState

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    shape: Shape = AppShapes.medium,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    contentPadding: PaddingValues = PaddingValues(AppSpacing.md),
    enableHoverElevation: Boolean = true,
    content: @Composable () -> Unit,
) {
    val surfaceColor = AppThemeState.semantic.cardBackground
    Box(
        modifier =
            modifier
                .then(
                    if (enableHoverElevation) {
                        Modifier.hoverableCard(shape = shape, interactionSource = interactionSource)
                    } else {
                        Modifier.cardShadow(shape = shape)
                    },
                )
                .clip(shape)
                .background(surfaceColor)
                .padding(contentPadding),
    ) {
        content()
    }
}
