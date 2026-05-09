package com.inventory.industry.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.inventory.industry.ui.theme.AppShapes
import com.inventory.industry.ui.theme.AppSpacing
import com.inventory.industry.ui.theme.AppTypography

@Composable
fun AppBasicDialog(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    confirmText: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissText: String? = null,
    onDismiss: (() -> Unit)? = null,
    shape: Shape = AppShapes.large,
    properties: DialogProperties = DialogProperties(),
    content: (@Composable () -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                if (dismissText != null) {
                    TextButton(onClick = onDismiss ?: onDismissRequest) { Text(dismissText) }
                }
                if (confirmText != null && onConfirm != null) {
                    TextButton(onClick = onConfirm) { Text(confirmText) }
                }
            }
        },
        modifier = modifier,
        title = { Text(text = title, style = AppTypography.SectionTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                message?.let {
                    Text(
                        text = it,
                        style = AppTypography.Body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                content?.invoke()
            }
        },
        shape = shape,
        properties = properties,
    )
}

@Composable
fun AppSurfaceDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = AppShapes.large,
    tonalElevation: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = modifier.fillMaxWidth().padding(AppSpacing.lg),
            shape = shape,
            tonalElevation = tonalElevation,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.padding(AppSpacing.lg)) {
                content()
            }
        }
    }
}
