package com.inventory.industry.ui.components.table

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inventory.industry.ui.components.buttons.AppIconButton
import com.inventory.industry.ui.components.inputs.AppDropdownField
import com.inventory.industry.ui.theme.AppSpacing
import com.inventory.industry.ui.theme.AppTypography

@Composable
fun ListPaginationFooter(
    page: Int,
    pageCount: Int,
    rowsPerPage: Int,
    onRowsPerPageChange: (Int) -> Unit,
    totalItems: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    pageSizes: List<Int> = listOf(10, 25, 50, 100),
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "$totalItems resultado" + if (totalItems != 1) "s" else "",
            style = AppTypography.BodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                Text(
                    "Filas / pág.",
                    style = AppTypography.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AppDropdownField(
                    label = null,
                    options = pageSizes,
                    selected = rowsPerPage,
                    onSelected = onRowsPerPageChange,
                    optionLabel = { "$it" },
                    modifier = Modifier.widthIn(min = 72.dp, max = 96.dp),
                )
            }
            Text(
                text = "${page + 1} / $pageCount",
                style = AppTypography.BodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AppIconButton(onClick = onPrev, enabled = page > 0, size = 40.dp) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Página anterior",
                    tint =
                        if (page > 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                        },
                )
            }
            AppIconButton(onClick = onNext, enabled = page < pageCount - 1, size = 40.dp) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Página siguiente",
                    tint =
                        if (page < pageCount - 1) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                        },
                )
            }
        }
    }
}
