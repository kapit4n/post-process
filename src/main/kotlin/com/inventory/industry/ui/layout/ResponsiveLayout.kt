package com.inventory.industry.ui.layout

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.inventory.industry.ui.theme.AppSpacing

@Composable
fun BoxWithWindowSize(
    modifier: Modifier = Modifier,
    content: @Composable (WindowSize) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val windowSize =
            remember(maxWidth, maxHeight) {
                WindowSize.fromSize(width = maxWidth, height = maxHeight)
            }
        content(windowSize)
    }
}

fun responsiveHorizontalPadding(windowSize: WindowSize): Dp =
    when (windowSize.widthClass) {
        WindowWidthClass.Compact -> AppSpacing.md
        WindowWidthClass.Medium -> AppSpacing.lg
        WindowWidthClass.Expanded -> AppSpacing.xl
    }

fun responsiveMaxContentWidth(windowSize: WindowSize): Dp? =
    when (windowSize.widthClass) {
        WindowWidthClass.Compact -> null
        WindowWidthClass.Medium -> 1200.dp
        WindowWidthClass.Expanded -> 1480.dp
    }
