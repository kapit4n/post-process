package com.inventory.industry.ui.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.inventory.industry.ui.theme.AppSpacing

/** Contenedor del panel sin scroll global: el contenido usa weight y scroll interno por pestaña. */
@Composable
fun DashboardLayout(
    @Suppress("UNUSED_PARAMETER") windowSize: WindowSize,
    modifier: Modifier = Modifier,
    verticalSpacing: Dp = AppSpacing.sm,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        content = content,
    )
}
