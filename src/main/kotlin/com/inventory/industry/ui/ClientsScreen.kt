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
import com.inventory.industry.data.Client
import com.inventory.industry.data.InventoryRepository
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
fun ClientsScreen(repo: InventoryRepository) {
    var rows by remember { mutableStateOf<List<Client>>(emptyList()) }
    var editor by remember { mutableStateOf<Client?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            rows = withContext(Dispatchers.IO) { repo.listClients() }
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
        title = "Clientes",
        subtitle = "Cuentas de venta para postes terminados o saldo (fallados).",
        showSearch = true,
        searchValue = search,
        onSearchChange = { search = it },
        searchPlaceholder = "Buscar cliente o contacto…",
        actions = {
            AppButton(
                text = "Nuevo cliente",
                onClick = {
                    creating = true
                    editor = null
                },
            )
        },
    ) {
        deleteError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = AppTypography.BodySmall)
        }
        val columns =
            listOf(
                AppTableColumn<Client>(
                    header = "Nombre",
                    weight = 1f,
                    cell = { c ->
                        Text(c.name, style = AppTypography.Body, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    },
                ),
                AppTableColumn(
                    header = "Contacto",
                    weight = 0.9f,
                    cell = { c ->
                        Text(
                            c.contact.orEmpty(),
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
                    cell = { c ->
                        Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                editor = c
                                creating = false
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar")
                            }
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        val ok =
                                            withContext(Dispatchers.IO) { repo.deleteClient(c.id) }
                                        if (!ok) {
                                            deleteError =
                                                "No se puede borrar: el cliente tiene ventas registradas."
                                        } else {
                                            deleteError = null
                                            reload()
                                        }
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
        ClientEditorDialog(
            initial = editor,
            onDismiss = {
                creating = false
                editor = null
            },
            onSave = { id, name, contact, notes ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        repo.upsertClient(id, name, contact, notes)
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
private fun ClientEditorDialog(
    initial: Client?,
    onDismiss: () -> Unit,
    onSave: (id: Int?, name: String, contact: String?, notes: String?) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var contact by remember { mutableStateOf(initial?.contact.orEmpty()) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nuevo cliente" else "Editar cliente") },
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
