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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.inventory.industry.data.ResourceStockLot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ResourcesScreen(repo: InventoryRepository) {
    var tab by remember { mutableStateOf(0) }
    var rows by remember { mutableStateOf<List<Resource>>(emptyList()) }
    var stockLots by remember { mutableStateOf<List<ResourceStockLot>>(emptyList()) }
    var stockValueEstimate by remember { mutableStateOf(0.0) }
    var editor by remember { mutableStateOf<Resource?>(null) }
    var creating by remember { mutableStateOf(false) }
    var stockEditor by remember { mutableStateOf<ResourceStockLot?>(null) }
    var creatingStock by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun reloadCatalog() {
        scope.launch {
            rows = withContext(Dispatchers.IO) { repo.listResources() }
        }
    }

    fun reloadStock() {
        scope.launch {
            stockLots = withContext(Dispatchers.IO) { repo.listResourceStockLots() }
            stockValueEstimate = withContext(Dispatchers.IO) { repo.resourceStockTotalValueEstimate() }
        }
    }

    LaunchedEffect(Unit) {
        reloadCatalog()
        reloadStock()
    }

    LaunchedEffect(tab) {
        if (tab == 1) reloadStock()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (tab) {
                        0 -> {
                            creating = true
                            editor = null
                        }
                        else -> {
                            creatingStock = true
                            stockEditor = null
                        }
                    }
                },
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar")
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
                "Insumos",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Catálogo") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Inventario (lotes)") })
            }
            when (tab) {
                0 -> ResourceCatalogTab(
                    rows = rows,
                    onEdit = {
                        editor = it
                        creating = false
                    },
                    onDelete = {
                        scope.launch {
                            withContext(Dispatchers.IO) { repo.deleteResource(it.id) }
                            reloadCatalog()
                            reloadStock()
                        }
                    },
                )
                else ->
                    ResourceStockInventoryTab(
                        stockLots = stockLots,
                        stockValueEstimate = stockValueEstimate,
                        onEdit = {
                            stockEditor = it
                            creatingStock = false
                        },
                        onDelete = {
                            scope.launch {
                                withContext(Dispatchers.IO) { repo.deleteResourceStockLot(it.id) }
                                reloadStock()
                            }
                        },
                    )
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
                    reloadCatalog()
                }
            },
        )
    }

    if (creatingStock || stockEditor != null) {
        ResourceStockLotEditorDialog(
            resources = rows,
            initial = stockEditor,
            onDismiss = {
                creatingStock = false
                stockEditor = null
            },
            onSave = { id, resourceId, quantity, acquisitionPrice, expirationText, notes ->
                scope.launch {
                    val exp = parseIsoDate(expirationText)
                    withContext(Dispatchers.IO) {
                        repo.upsertResourceStockLot(
                            id = id,
                            resourceId = resourceId,
                            quantity = quantity,
                            acquisitionPricePerUnit = acquisitionPrice,
                            expirationDate = exp,
                            notes = notes?.trim()?.ifBlank { null },
                        )
                    }
                    creatingStock = false
                    stockEditor = null
                    reloadStock()
                }
            },
        )
    }
}

@Composable
private fun ResourceCatalogTab(
    rows: List<Resource>,
    onEdit: (Resource) -> Unit,
    onDelete: (Resource) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Catálogo maestro: nombre, unidad de medida y costo de referencia por unidad " +
                "(se usa al registrar transformaciones si no descuenta aún del inventario por lote).",
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Nombre", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.SemiBold)
            Text("Unidad", modifier = Modifier.weight(0.6f), fontWeight = FontWeight.SemiBold)
            Text("Costo / ud.", modifier = Modifier.weight(0.8f), fontWeight = FontWeight.SemiBold)
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
                        IconButton(onClick = { onEdit(r) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar")
                        }
                        IconButton(onClick = { onDelete(r) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                        }
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ResourceStockInventoryTab(
    stockLots: List<ResourceStockLot>,
    stockValueEstimate: Double,
    onEdit: (ResourceStockLot) -> Unit,
    onDelete: (ResourceStockLot) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Registre cada compra o partida: cantidad en la unidad del insumo, precio de adquisición " +
                "por esa unidad y fecha de vencimiento si aplica.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "Valor estimado del inventario registrado: ${formatMoney(stockValueEstimate)}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Insumo", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.SemiBold)
            Text("Cantidad", modifier = Modifier.weight(0.55f), fontWeight = FontWeight.SemiBold)
            Text("Precio / ud.", modifier = Modifier.weight(0.65f), fontWeight = FontWeight.SemiBold)
            Text("Vence", modifier = Modifier.weight(0.65f), fontWeight = FontWeight.SemiBold)
            Text("", modifier = Modifier.weight(0.5f))
        }
        HorizontalDivider()
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(stockLots, key = { it.id }) { lot ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text(lot.resourceName, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            lot.notes.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }
                    Text(
                        "${formatQty(lot.quantity)} ${lot.resourceUnit}",
                        modifier = Modifier.weight(0.55f),
                    )
                    Text(formatMoney(lot.acquisitionPricePerUnit), modifier = Modifier.weight(0.65f))
                    Text(formatIsoDateOrDash(lot.expirationDate), modifier = Modifier.weight(0.65f))
                    Row(
                        modifier = Modifier.weight(0.5f),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        IconButton(onClick = { onEdit(lot) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar")
                        }
                        IconButton(onClick = { onDelete(lot) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                        }
                    }
                }
                HorizontalDivider()
            }
        }
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
                TextField(value = cost, onValueChange = { cost = it }, label = { Text("Costo por unidad (referencia)") })
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

@Composable
private fun ResourceStockLotEditorDialog(
    resources: List<Resource>,
    initial: ResourceStockLot?,
    onDismiss: () -> Unit,
    onSave: (
        id: Int?,
        resourceId: Int,
        quantity: Double,
        acquisitionPrice: Double,
        expirationText: String,
        notes: String?,
    ) -> Unit,
) {
    var resourceId by remember(initial?.id) {
        mutableStateOf(initial?.resourceId ?: resources.firstOrNull()?.id)
    }
    var qtyText by remember { mutableStateOf(initial?.quantity?.toString() ?: "") }
    var priceText by remember { mutableStateOf(initial?.acquisitionPricePerUnit?.toString() ?: "") }
    var expirationText by remember {
        mutableStateOf(initial?.expirationDate?.let { formatIsoDate(it) } ?: "")
    }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Entrada de inventario" else "Editar partida") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (resources.isEmpty()) {
                    Text(
                        "Cree primero un insumo en la pestaña Catálogo.",
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Text("Insumo", style = MaterialTheme.typography.labelLarge)
                    CycleOrDropdownPicker(
                        items = resources,
                        selected = resources.firstOrNull { it.id == resourceId },
                        onSelected = { resourceId = it.id },
                        labelFor = { "${it.name} (${it.unit})" },
                        placeholder = "Elegir…",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextField(
                        value = qtyText,
                        onValueChange = { qtyText = it },
                        label = { Text("Cantidad (en la unidad del insumo)") },
                    )
                    TextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Precio de adquisición por unidad") },
                    )
                    TextField(
                        value = expirationText,
                        onValueChange = { expirationText = it },
                        label = { Text("Vencimiento (yyyy-MM-dd, opcional)") },
                    )
                    TextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notas (lote, factura, …)") },
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rid = resourceId ?: return@Button
                    val q = qtyText.toDoubleOrNull() ?: return@Button
                    val p = priceText.toDoubleOrNull() ?: return@Button
                    if (q <= 0 || p < 0) return@Button
                    onSave(initial?.id, rid, q, p, expirationText, notes)
                },
                enabled = resources.isNotEmpty(),
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
