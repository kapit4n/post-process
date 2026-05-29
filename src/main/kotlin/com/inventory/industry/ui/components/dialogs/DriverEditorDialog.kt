package com.inventory.industry.ui.components.dialogs

import androidx.compose.runtime.Composable
import com.inventory.industry.data.Driver

@Composable
fun DriverEditorDialog(
    initial: Driver? = null,
    onDismiss: () -> Unit,
    onSave: (id: Int?, name: String, phone: String?, notes: String?) -> Unit,
) {
    EntityEditorDialog(
        newTitle = "Nuevo chofer",
        editTitle = "Editar chofer",
        isNew = initial == null,
        fields = listOf(
            EntityEditorField("Nombre", initial?.name.orEmpty()),
            EntityEditorField("Teléfono (opcional)", initial?.phone.orEmpty()),
            EntityEditorField("Notas", initial?.notes.orEmpty()),
        ),
        onDismiss = onDismiss,
        onSave = { vals ->
            onSave(
                initial?.id,
                vals[0],
                vals.getOrNull(1)?.ifBlank { null },
                vals.getOrNull(2)?.ifBlank { null },
            )
        },
    )
}
