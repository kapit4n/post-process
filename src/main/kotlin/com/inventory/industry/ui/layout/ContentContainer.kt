package com.inventory.industry.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.inventory.industry.ui.theme.AppSpacing

@Composable
fun ContentContainer(
    windowSize: WindowSize,
    modifier: Modifier = Modifier,
    maxContentWidth: Dp? = responsiveMaxContentWidth(windowSize),
    horizontalPadding: Dp = responsiveHorizontalPadding(windowSize),
    verticalPadding: Dp = AppSpacing.md,
    contentAlignment: Alignment = Alignment.TopCenter,
    content: @Composable () -> Unit,
) {
    val widthModifier =
        if (maxContentWidth != null) {
            Modifier.widthIn(max = maxContentWidth).fillMaxWidth()
        } else {
            Modifier.fillMaxWidth()
        }
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = contentAlignment,
    ) {
        Box(modifier = widthModifier.fillMaxSize()) {
            content()
        }
    }
}
