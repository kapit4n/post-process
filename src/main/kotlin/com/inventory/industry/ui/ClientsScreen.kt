package com.inventory.industry.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inventory.industry.data.Client
import com.inventory.industry.data.InventoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ClientsScreen(repo: InventoryRepository) {
    var rows by remember { mutableStateOf<List<Client>>(emptyList()) }
    var editor by remember { mutableStateOf<Client?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            rows = withContext(Dispatchers.IO) { repo.listClients() }
        }
    }

    LaunchedEffect(Unit) {
        reload()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    creating = true
                    editor = null
                },
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar cliente")
            }
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Clientes",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Los clientes se eligen al registrar una venta de postes terminados o de saldo (fallados).",
                style = MaterialTheme.typography.bodyMedium,
            )
            deleteError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Nombre", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Text("Contacto", modifier = Modifier.weight(0.9f), fontWeight = FontWeight.SemiBold)
                Text("", modifier = Modifier.weight(0.45f))
            }
            HorizontalDivider()
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(rows, key = { it.id }) { c ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(c.name, modifier = Modifier.weight(1f), maxLines = 2)
                        Text(c.contact.orEmpty(), modifier = Modifier.weight(0.9f), maxLines = 2)
                        Row(
                            modifier = Modifier.weight(0.45f),
                            horizontalArrangement = Arrangement.End,
                        ) {
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
                    }
                    HorizontalDivider()
                }
            }
        }
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") })
                TextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Contacto (opcional)") },
                )
                TextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas (opcional)") },
                )
            }
        },
        confirmButton = {
            Button(
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
