package com.inventory.industry.ui.components.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.inventory.industry.ui.theme.AppShapes
import com.inventory.industry.ui.theme.AppSpacing
import com.inventory.industry.ui.theme.AppTypography

@Composable
fun InfoCard(
    title: String,
    modifier: Modifier = Modifier,
    shape: Shape = AppShapes.medium,
    contentPadding: PaddingValues = PaddingValues(AppSpacing.lg),
    leading: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
    body: @Composable () -> Unit,
) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        contentPadding = contentPadding,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
            ) {
                leading?.invoke()
                Text(
                    text = title,
                    style = AppTypography.CardTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                actions?.invoke()
            }
            body()
        }
    }
}
