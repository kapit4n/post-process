package com.inventory.industry.ui.components.table

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inventory.industry.ui.modifiers.smoothClickable
import com.inventory.industry.ui.theme.AppShapes
import com.inventory.industry.ui.theme.AppSpacing
import com.inventory.industry.ui.theme.AppTypography

data class AppTableColumn<T>(
    val header: String,
    val weight: Float = 1f,
    val cell: @Composable (T) -> Unit,
)

@Composable
fun TableToolbar(
    modifier: Modifier = Modifier,
    leading: (@Composable RowScope.() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        leading?.invoke(this)
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            actions?.invoke(this)
        }
    }
}

@Composable
fun <T> AppDataTable(
    items: List<T>,
    columns: List<AppTableColumn<T>>,
    modifier: Modifier = Modifier,
    key: (T) -> Any,
    onRowClick: ((T) -> Unit)? = null,
    emptyMessage: String = "Sin filas para mostrar",
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                columns.forEach { col ->
                    Text(
                        text = col.header,
                        style = AppTypography.Caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(col.weight),
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (items.isEmpty()) {
                Text(
                    emptyMessage,
                    style = AppTypography.Body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(AppSpacing.xl),
                )
            } else {
                LazyColumn {
                    itemsIndexed(items, key = { _, item -> key(item) }) { index, item ->
                        val stripe =
                            if (index % 2 == 0) {
                                MaterialTheme.colorScheme.surface
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
                            }
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(stripe)
                                    .then(
                                        if (onRowClick != null) {
                                            Modifier.smoothClickable(onClick = { onRowClick.invoke(item) })
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            columns.forEach { col ->
                                Row(
                                    Modifier.weight(col.weight),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    col.cell(item)
                                }
                            }
                        }
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        )
                    }
                }
            }
        }
    }
}
