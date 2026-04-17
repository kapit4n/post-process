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
import com.inventory.industry.data.InventoryRepository
import com.inventory.industry.data.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ResourcesScreen(repo: InventoryRepository) {
    var rows by remember { mutableStateOf<List<Resource>>(emptyList()) }
    var editor by remember { mutableStateOf<Resource?>(null) }
    var creating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            rows = withContext(Dispatchers.IO) { repo.listResources() }
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
                Icon(Icons.Default.Add, contentDescription = "Agregar insumo")
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
                "Insumos (líquidos, preservantes, químicos…)",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "El costo unitario se usa al avanzar un lote a la siguiente etapa " +
                    "(p. ej. creosota al Tratar, o combustible al Secar).",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Nombre", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.SemiBold)
                Text("Unidad", modifier = Modifier.weight(0.6f), fontWeight = FontWeight.SemiBold)
                Text("Costo / unidad", modifier = Modifier.weight(0.8f), fontWeight = FontWeight.SemiBold)
                Text("", modifier = Modifier.weight(0.6f))
            }
            HorizontalDivider()
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(rows, key = { it.id }) { r ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(r.name, modifier = Modifier.weight(1.5f))
                        Text(r.unit, modifier = Modifier.weight(0.6f))
                        Text("%.2f".format(r.costPerUnit), modifier = Modifier.weight(0.8f))
                        Row(
                            modifier = Modifier.weight(0.6f),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            IconButton(onClick = {
                                editor = r
                                creating = false
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar")
                            }
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) { repo.deleteResource(r.id) }
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
        ResourceEditorDialog(
            initial = editor,
            onDismiss = {
                creating = false
                editor = null
            },
            onSave = { id, name, unit, cost ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        repo.upsertResource(id, name, unit, cost)
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
private fun ResourceEditorDialog(
    initial: Resource?,
    onDismiss: () -> Unit,
    onSave: (id: Int?, name: String, unit: String, cost: Double) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var unit by remember { mutableStateOf(initial?.unit.orEmpty()) }
    var cost by remember { mutableStateOf(initial?.costPerUnit?.toString().orEmpty().ifEmpty { "0" }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nuevo insumo" else "Editar insumo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") })
                TextField(value = unit, onValueChange = { unit = it }, label = { Text("Unidad (L, kg, m³, …)") })
                TextField(value = cost, onValueChange = { cost = it }, label = { Text("Costo por unidad") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val v = cost.toDoubleOrNull() ?: 0.0
                    onSave(initial?.id, name.trim(), unit.trim(), v)
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
