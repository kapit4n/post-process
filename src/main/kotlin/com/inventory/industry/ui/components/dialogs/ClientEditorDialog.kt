package com.inventory.industry.ui.components.dialogs

import androidx.compose.runtime.Composable
import com.inventory.industry.data.Client

@Composable
fun ClientEditorDialog(
    initial: Client? = null,
    onDismiss: () -> Unit,
    onSave: (id: Int?, name: String, contact: String?, notes: String?) -> Unit,
) {
    EntityEditorDialog(
        newTitle = "Nuevo cliente",
        editTitle = "Editar cliente",
        isNew = initial == null,
        fields = listOf(
            EntityEditorField("Nombre", initial?.name.orEmpty()),
            EntityEditorField("Contacto (tel., correo…)", initial?.contact.orEmpty()),
            EntityEditorField("Notas (opcional)", initial?.notes.orEmpty(), multiline = true),
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
