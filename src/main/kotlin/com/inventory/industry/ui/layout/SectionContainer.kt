package com.inventory.industry.ui.layout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.inventory.industry.ui.theme.AppSpacing

@Composable
fun SectionContainer(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = AppSpacing.lg,
    verticalPadding: Dp = AppSpacing.md,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        content = content,
    )
}
