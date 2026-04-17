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
import com.inventory.industry.data.CatalogProduct
import com.inventory.industry.data.InventoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CatalogScreen(repo: InventoryRepository) {
    var rows by remember { mutableStateOf<List<CatalogProduct>>(emptyList()) }
    var editor by remember { mutableStateOf<CatalogProduct?>(null) }
    var creating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            rows = withContext(Dispatchers.IO) { repo.listCatalogProducts() }
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
                Icon(Icons.Default.Add, contentDescription = "Agregar al catálogo")
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
                "Catálogo de productos",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Defina los tipos de poste que maneja (p. ej. poste de pino 9 m, poste de eucalipto 12 m). " +
                    "Al ingresar un lote Crudo se elige uno de esta lista para precargar nombre y línea.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Nombre", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.SemiBold)
                Text("Línea", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Text("Descripción", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.SemiBold)
                Text("", modifier = Modifier.weight(0.5f))
            }
            HorizontalDivider()
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(rows, key = { it.id }) { c ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(c.name, modifier = Modifier.weight(1.2f))
                        Text(c.productLine, modifier = Modifier.weight(1f))
                        Text(c.description.orEmpty(), modifier = Modifier.weight(1.2f), maxLines = 2)
                        Row(
                            modifier = Modifier.weight(0.5f),
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
                                        withContext(Dispatchers.IO) { repo.deleteCatalogProduct(c.id) }
                                        reload()
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
        CatalogEditorDialog(
            initial = editor,
            onDismiss = {
                creating = false
                editor = null
            },
            onSave = { id, name, line, desc ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        repo.upsertCatalogProduct(id, name, line, desc)
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
private fun CatalogEditorDialog(
    initial: CatalogProduct?,
    onDismiss: () -> Unit,
    onSave: (id: Int?, name: String, line: String, description: String?) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var line by remember { mutableStateOf(initial?.productLine.orEmpty()) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nuevo producto del catálogo" else "Editar producto del catálogo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Nombre (p. ej. Poste Pino 9m)") })
                TextField(value = line, onValueChange = { line = it }, label = { Text("Línea (p. ej. Pino, Eucalipto)") })
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción (opcional)") },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initial?.id,
                        name.trim(),
                        line.trim(),
                        description.trim().ifBlank { null },
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
