package com.inventory.industry.ui.modifiers

import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun Modifier.trackHover(
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onHoverChange: ((Boolean) -> Unit)? = null,
): Modifier {
    val isHovered by interactionSource.collectIsHoveredAsState()
    LaunchedEffect(isHovered, onHoverChange) {
        onHoverChange?.invoke(isHovered)
    }
    return this.hoverable(interactionSource)
}

@Composable
fun rememberHoverInteractionSource(): MutableInteractionSource = remember { MutableInteractionSource() }

@Composable
fun Modifier.hoverSurface(
    interactionSource: MutableInteractionSource = rememberHoverInteractionSource(),
    onHoverChange: ((Boolean) -> Unit)? = null,
): Modifier = this.trackHover(interactionSource, onHoverChange)
