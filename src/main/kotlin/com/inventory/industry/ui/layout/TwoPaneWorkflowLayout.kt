package com.inventory.industry.ui.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.inventory.industry.ui.theme.AppSpacing

@Composable
fun TwoPaneWorkflowLayout(
    modifier: Modifier = Modifier,
    breakpoint: Dp = 980.dp,
    mainWeight: Float = 1.5f,
    sideWeight: Float = 1f,
    verticalSpacing: Dp = AppSpacing.lg,
    horizontalSpacing: Dp = AppSpacing.lg,
    main: @Composable (Modifier) -> Unit,
    side: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth < breakpoint) {
            Column(verticalArrangement = Arrangement.spacedBy(verticalSpacing)) {
                main(Modifier.fillMaxWidth())
                side(Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
                verticalAlignment = Alignment.Top,
            ) {
                main(Modifier.weight(mainWeight))
                side(Modifier.weight(sideWeight))
            }
        }
    }
}
