package com.inventory.industry.ui.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.inventory.industry.ui.theme.AppSpacing

@Composable
fun FormLayout(
    windowSize: WindowSize,
    modifier: Modifier = Modifier,
    maxFormWidth: Dp = 640.dp,
    verticalSpacing: Dp = AppSpacing.md,
    content: @Composable ColumnScope.() -> Unit,
) {
    val max =
        when (windowSize.widthClass) {
            WindowWidthClass.Compact -> maxFormWidth
            WindowWidthClass.Medium -> maxFormWidth + 80.dp
            WindowWidthClass.Expanded -> maxFormWidth + 120.dp
        }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .widthIn(max = max),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        content = content,
    )
}
