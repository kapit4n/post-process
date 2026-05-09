package com.inventory.industry.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inventory.industry.data.InventoryRepository
import com.inventory.industry.data.PoleProvider
import com.inventory.industry.ui.components.buttons.AppButton
import com.inventory.industry.ui.components.inputs.AppTextField
import com.inventory.industry.ui.components.table.AppDataTable
import com.inventory.industry.ui.components.table.AppTableColumn
import com.inventory.industry.ui.layout.EnterpriseScreenLayout
import com.inventory.industry.ui.theme.AppTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProvidersScreen(repo: InventoryRepository) {
    var rows by remember { mutableStateOf<List<PoleProvider>>(emptyList()) }
    var editor by remember { mutableStateOf<PoleProvider?>(null) }
    var creating by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            rows = withContext(Dispatchers.IO) { repo.listPoleProviders() }
        }
    }

    LaunchedEffect(Unit) {
        reload()
    }

    val filtered by remember {
        derivedStateOf {
            val q = search.trim().lowercase()
            if (q.isEmpty()) rows
            else rows.filter { it.name.lowercase().contains(q) || (it.contact?.lowercase()?.contains(q) == true) }
        }
    }

    EnterpriseScreenLayout(
        title = "Proveedores de postes",
        subtitle =
            "Registre de quién proviene la madera en etapa Crudo. Vincule al crear o editar lotes.",
        showSearch = true,
        searchValue = search,
        onSearchChange = { search = it },
        searchPlaceholder = "Buscar proveedor…",
        actions = {
            AppButton(
                text = "Nuevo proveedor",
                onClick = {
                    creating = true
                    editor = null
                },
            )
        },
    ) {
        val columns =
            listOf(
                AppTableColumn<PoleProvider>(
                    header = "Nombre",
                    weight = 1f,
                    cell = { p ->
                        Text(p.name, style = AppTypography.Body, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    },
                ),
                AppTableColumn(
                    header = "Contacto",
                    weight = 0.9f,
                    cell = { p ->
                        Text(
                            p.contact.orEmpty(),
                            style = AppTypography.BodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                ),
                AppTableColumn(
                    header = "",
                    weight = 0.45f,
                    cell = { p ->
                        Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                editor = p
                                creating = false
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar")
                            }
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) { repo.deletePoleProvider(p.id) }
                                        reload()
                                    }
                                },
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                            }
                        }
                    },
                ),
            )
        AppDataTable(items = filtered, columns = columns, key = { it.id })
    }

    if (creating || editor != null) {
        ProviderEditorDialog(
            initial = editor,
            onDismiss = {
                creating = false
                editor = null
            },
            onSave = { id, name, contact, notes ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        repo.upsertPoleProvider(id, name, contact, notes)
                    }
                    creating = false
                    editor = null
                    reload()
                }
            },
        )
    }
}

@Composable
private fun ProviderEditorDialog(
    initial: PoleProvider?,
    onDismiss: () -> Unit,
    onSave: (id: Int?, name: String, contact: String?, notes: String?) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var contact by remember { mutableStateOf(initial?.contact.orEmpty()) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nuevo proveedor" else "Editar proveedor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppTextField(value = name, onValueChange = { name = it }, label = "Nombre")
                AppTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = "Contacto (tel., correo…)",
                )
                AppTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Notas (opcional)",
                    maxLines = 4,
                    singleLine = false,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        initial?.id,
                        name.trim(),
                        contact.trim().ifBlank { null },
                        notes.trim().ifBlank { null },
                    )
                },
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
