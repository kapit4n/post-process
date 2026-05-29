package com.inventory.industry.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inventory.industry.ui.components.inputs.AppTextField
import com.inventory.industry.ui.theme.AppSpacing

data class EntityEditorField(
    val label: String,
    val initialValue: String = "",
    val multiline: Boolean = false,
)

@Composable
fun EntityEditorDialog(
    newTitle: String,
    editTitle: String,
    isNew: Boolean,
    fields: List<EntityEditorField>,
    onDismiss: () -> Unit,
    onSave: (values: List<String>) -> Unit,
    requireFirstField: Boolean = true,
) {
    var values by remember(fields) { mutableStateOf(fields.map { it.initialValue }) }

    val firstOk = !requireFirstField || values.firstOrNull()?.trim()?.isNotEmpty() == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) newTitle else editTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                fields.forEachIndexed { index, field ->
                    AppTextField(
                        value = values.getOrElse(index) { "" },
                        onValueChange = { new ->
                            values = values.toMutableList().also { it[index] = new }
                        },
                        label = field.label,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = if (field.multiline) 4 else 1,
                        singleLine = !field.multiline,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(values.map { it.trim() }) },
                enabled = firstOk,
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
