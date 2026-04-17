package com.inventory.industry.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inventory.industry.data.CatalogProduct
import com.inventory.industry.data.InventoryRepository
import com.inventory.industry.data.Product
import com.inventory.industry.data.Resource
import com.inventory.industry.domain.ProductStage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val failureRecordStages =
    listOf(ProductStage.CRUDO, ProductStage.DESCORTEZADO, ProductStage.TRATADO)

@Composable
fun ProductsByStageScreen(repo: InventoryRepository) {
    var tab by remember { mutableStateOf(0) }
    val stages = ProductStage.entries
    val stage = stages[tab]
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Product?>(null) }
    var transformTrigger by remember { mutableStateOf<TransformTrigger?>(null) }
    var markFailedTarget by remember { mutableStateOf<Product?>(null) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            products =
                withContext(Dispatchers.IO) {
                    repo.listProducts(stage)
                }
        }
    }

    LaunchedEffect(stage) {
        reload()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editing = null
                    showEditor = true
                },
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar lote")
            }
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Postes por etapa",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Use \"Procesar etapa\" para tomar postes de uno o varios lotes y pasarlos a " +
                    "la siguiente etapa, registrando cuántos salieron bien y cuántos fallaron.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TabRow(selectedTabIndex = tab) {
                stages.forEachIndexed { index, s ->
                    Tab(
                        selected = tab == index,
                        onClick = { tab = index },
                        text = { Text(s.shortCode) },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stage.title, style = MaterialTheme.typography.bodyMedium)
                if (stage.next() != null) {
                    Button(
                        onClick = { transformTrigger = TransformTrigger(preselectId = null) },
                        enabled = products.any { !it.isFailed },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        Text(
                            "  Procesar etapa → ${stage.next()!!.shortCode}",
                        )
                    }
                }
            }

            ProductTable(
                products = products,
                showAdvance = stage.next() != null,
                onEdit = {
                    editing = it
                    showEditor = true
                },
                onDelete = {
                    scope.launch {
                        withContext(Dispatchers.IO) { repo.deleteProduct(it.id) }
                        reload()
                    }
                },
                onAdvance = {
                    transformTrigger = TransformTrigger(preselectId = it.id)
                },
                onMarkFailed = {
                    markFailedTarget = it
                },
                onClearFailure = {
                    scope.launch {
                        withContext(Dispatchers.IO) { repo.clearProductFailure(it.id) }
                        reload()
                    }
                },
            )
        }
    }

    if (showEditor) {
        ProductEditorDialog(
            repo = repo,
            initial = editing,
            defaultStage = stage,
            onDismiss = { showEditor = false },
            onSave = { draft ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        repo.upsertProduct(
                            id = draft.id,
                            name = draft.name,
                            productLine = draft.productLine,
                            stage = draft.stage,
                            quantity = draft.quantity,
                            notes = draft.notes,
                            catalogProductId = draft.catalogProductId,
                            standardSalePrice = draft.standardSalePrice,
                            failedSalePrice = draft.failedSalePrice,
                        )
                    }
                    showEditor = false
                    reload()
                }
            },
        )
    }

    transformTrigger?.let { trigger ->
        TransformationDialog(
            repo = repo,
            fromStage = stage,
            availableBatches = products.filter { !it.isFailed },
            preselectId = trigger.preselectId,
            onDismiss = { transformTrigger = null },
            onDone = {
                transformTrigger = null
                reload()
            },
        )
    }

    markFailedTarget?.let { product ->
        MarkFailedDialog(
            product = product,
            onDismiss = { markFailedTarget = null },
            onConfirm = { atStage, failedPrice, noteExtra ->
                markFailedTarget = null
                scope.launch {
                    withContext(Dispatchers.IO) {
                        repo.markProductFailed(product.id, atStage, failedPrice, noteExtra)
                    }
                    reload()
                }
            },
        )
    }
}

private data class TransformTrigger(val preselectId: Int?)

data class ProductDraft(
    val id: Int?,
    val name: String,
    val productLine: String,
    val stage: ProductStage,
    val quantity: Double,
    val notes: String?,
    val catalogProductId: Int?,
    val standardSalePrice: Double?,
    val failedSalePrice: Double?,
)

@Composable
private fun ProductTable(
    products: List<Product>,
    showAdvance: Boolean,
    onEdit: (Product) -> Unit,
    onDelete: (Product) -> Unit,
    onAdvance: (Product) -> Unit,
    onMarkFailed: (Product) -> Unit,
    onClearFailure: (Product) -> Unit,
) {
    Column {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TableHeaderCell("Nombre", 1.1f)
            TableHeaderCell("Línea", 0.75f)
            TableHeaderCell("Cant.", 0.35f)
            TableHeaderCell("Precio", 0.55f)
            TableHeaderCell("Estado", 0.65f)
            TableHeaderCell("Notas", 0.9f)
            TableHeaderCell("Acc.", 0.95f)
        }
        HorizontalDivider()
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(products, key = { it.id }) { p ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    TableCell(p.name, 1.1f)
                    TableCell(p.productLine, 0.75f)
                    TableCell(formatQty(p.quantity), 0.35f)
                    TableCell(
                        p.effectiveSalePrice()?.let { formatMoney(it) } ?: "—",
                        0.55f,
                    )
                    val statusColor =
                        if (p.isFailed) MaterialTheme.colorScheme.error else Color.Unspecified
                    Text(
                        p.statusLabel(),
                        modifier = Modifier.weight(0.65f),
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor,
                        fontWeight = if (p.isFailed) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    TableCell(p.notes.orEmpty(), 0.9f)
                    Row(
                        modifier = Modifier.weight(0.95f),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { onEdit(p) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar")
                        }
                        if (p.stage != ProductStage.TERMINADO && !p.isFailed) {
                            IconButton(onClick = { onMarkFailed(p) }) {
                                Icon(Icons.Default.Warning, contentDescription = "Marcar fallado")
                            }
                        }
                        if (p.isFailed) {
                            IconButton(onClick = { onClearFailure(p) }) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Quitar falla")
                            }
                        }
                        if (showAdvance && !p.isFailed) {
                            IconButton(onClick = { onAdvance(p) }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Procesar este lote",
                                )
                            }
                        }
                        IconButton(onClick = { onDelete(p) }) {
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
private fun RowScope.TableHeaderCell(
    text: String,
    weight: Float,
) {
    Text(
        text,
        modifier = Modifier.weight(weight),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun RowScope.TableCell(
    text: String,
    weight: Float,
) {
    Text(text, modifier = Modifier.weight(weight), style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun ProductEditorDialog(
    repo: InventoryRepository,
    initial: Product?,
    defaultStage: ProductStage,
    onDismiss: () -> Unit,
    onSave: (ProductDraft) -> Unit,
) {
    var catalog by remember { mutableStateOf<List<CatalogProduct>>(emptyList()) }
    var catalogProductId by remember { mutableStateOf(initial?.catalogProductId) }
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var line by remember { mutableStateOf(initial?.productLine.orEmpty()) }
    var stage by remember { mutableStateOf(initial?.stage ?: defaultStage) }
    var qty by remember { mutableStateOf(initial?.quantity?.toString() ?: "1") }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }
    var standardPrice by remember { mutableStateOf(initial?.standardSalePrice?.toString() ?: "") }
    var failedPrice by remember { mutableStateOf(initial?.failedSalePrice?.toString() ?: "") }

    LaunchedEffect(Unit) {
        catalog = withContext(Dispatchers.IO) { repo.listCatalogProducts() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nuevo lote de postes" else "Editar lote") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (catalog.isNotEmpty()) {
                    Text("Desde el catálogo", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CatalogPicker(
                            catalog = catalog,
                            selectedId = catalogProductId,
                            onSelect = { id, item ->
                                catalogProductId = id
                                if (item != null) {
                                    name = item.name
                                    line = item.productLine
                                }
                            },
                        )
                        TextButton(
                            onClick = {
                                catalogProductId = null
                            },
                        ) {
                            Text("Quitar vínculo")
                        }
                    }
                }
                TextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") })
                TextField(value = line, onValueChange = { line = it }, label = { Text("Línea de producto") })
                Text("Etapa", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProductStage.entries.forEach { s ->
                        OutlinedButton(
                            onClick = { stage = s },
                        ) {
                            Text(
                                s.shortCode,
                                fontWeight = if (stage == s) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
                TextField(value = qty, onValueChange = { qty = it }, label = { Text("Cantidad") })
                TextField(
                    value = standardPrice,
                    onValueChange = { standardPrice = it },
                    label = { Text("Precio de venta estándar (opcional)") },
                )
                TextField(
                    value = failedPrice,
                    onValueChange = { failedPrice = it },
                    label = { Text("Precio de saldo / fallado (opcional)") },
                )
                TextField(value = notes, onValueChange = { notes = it }, label = { Text("Notas") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val q = qty.toDoubleOrNull() ?: 1.0
                    onSave(
                        ProductDraft(
                            id = initial?.id,
                            name = name.trim(),
                            productLine = line.trim(),
                            stage = stage,
                            quantity = q,
                            notes = notes.ifBlank { null },
                            catalogProductId = catalogProductId,
                            standardSalePrice = standardPrice.toDoubleOrNull(),
                            failedSalePrice = failedPrice.toDoubleOrNull(),
                        ),
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

@Composable
private fun RowScope.CatalogPicker(
    catalog: List<CatalogProduct>,
    selectedId: Int?,
    onSelect: (Int?, CatalogProduct?) -> Unit,
) {
    val current = catalog.firstOrNull { it.id == selectedId }
    OutlinedButton(
        onClick = {
            if (catalog.isEmpty()) return@OutlinedButton
            val idx = catalog.indexOfFirst { it.id == selectedId }.let { if (it < 0) 0 else it }
            val next = catalog[(idx + 1) % catalog.size]
            onSelect(next.id, next)
        },
        modifier = Modifier.weight(1f),
    ) {
        Text(current?.let { "${it.name} · ${it.productLine}" } ?: "Elegir del catálogo…")
    }
}

@Composable
private fun MarkFailedDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (ProductStage, Double, String?) -> Unit,
) {
    var failedAt by remember {
        mutableStateOf(
            when (product.stage) {
                ProductStage.CRUDO, ProductStage.DESCORTEZADO, ProductStage.TRATADO -> product.stage
                ProductStage.TERMINADO -> ProductStage.TRATADO
            },
        )
    }
    var price by remember {
        mutableStateOf(
            product.failedSalePrice?.toString()
                ?: product.standardSalePrice?.toString()
                ?: "",
        )
    }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Marcar como fallado: ${product.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "El lote permanece en la etapa ${product.stage.shortCode}. " +
                        "Indique en qué fase falló el procesamiento y el precio de saldo.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text("Falla atribuida a", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    failureRecordStages.forEach { s ->
                        OutlinedButton(onClick = { failedAt = s }) {
                            Text(
                                s.shortCode,
                                fontWeight = if (failedAt == s) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
                TextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Precio de venta de saldo") },
                )
                TextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota (se agrega a las existentes)") },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = price.toDoubleOrNull() ?: return@Button
                    onConfirm(failedAt, p, note.ifBlank { null })
                },
                enabled = price.toDoubleOrNull() != null,
            ) {
                Text("Marcar fallado")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
private fun TransformationDialog(
    repo: InventoryRepository,
    fromStage: ProductStage,
    availableBatches: List<Product>,
    preselectId: Int?,
    onDismiss: () -> Unit,
    onDone: () -> Unit,
) {
    val toStage = fromStage.next() ?: return

    val selected = remember { mutableStateMapOf<Int, Boolean>() }
    val takeQtyText = remember { mutableStateMapOf<Int, String>() }

    LaunchedEffect(preselectId, availableBatches) {
        if (preselectId != null && availableBatches.any { it.id == preselectId }) {
            selected[preselectId] = true
            val b = availableBatches.first { it.id == preselectId }
            takeQtyText[preselectId] = formatQty(b.quantity)
        }
    }

    val totalInput by remember(availableBatches) {
        derivedStateOf {
            availableBatches.sumOf { b ->
                if (selected[b.id] == true) {
                    takeQtyText[b.id]?.toDoubleOrNull() ?: 0.0
                } else {
                    0.0
                }
            }
        }
    }

    var successText by remember { mutableStateOf("") }
    var failedText by remember { mutableStateOf("0") }
    var whenText by remember { mutableStateOf(formatEpochMs(System.currentTimeMillis())) }
    var durationText by remember { mutableStateOf("60") }
    var notes by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(totalInput) {
        if (successText.isBlank() || successText.toDoubleOrNull() == null) {
            successText = formatQty(totalInput)
        }
    }

    var resources by remember { mutableStateOf<List<Resource>>(emptyList()) }
    val resourceLines = remember { mutableStateListOf<Triple<Int, String, String>>() }
    LaunchedEffect(Unit) {
        resources = withContext(Dispatchers.IO) { repo.listResources() }
    }

    val scope = rememberCoroutineScope()
    val successValue = successText.toDoubleOrNull()
    val failedValue = failedText.toDoubleOrNull()
    val distributedOk =
        successValue != null && failedValue != null &&
            kotlin.math.abs((successValue + failedValue) - totalInput) < 1e-6
    val whenValid = parseDateTime(whenText) != null
    val canSubmit =
        availableBatches.any { selected[it.id] == true } &&
            totalInput > 0.0 &&
            distributedOk &&
            whenValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Procesar ${fromStage.shortCode} → ${toStage.shortCode}",
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Marque los lotes de origen y cuántos postes tomar de cada uno. " +
                        "Indique luego cuántos salieron bien y cuántos fallaron.",
                    style = MaterialTheme.typography.bodySmall,
                )

                if (availableBatches.isEmpty()) {
                    Text(
                        "No hay lotes disponibles en ${fromStage.shortCode}.",
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Text("Lotes origen", style = MaterialTheme.typography.labelLarge)
                    availableBatches.forEach { b ->
                        BatchRow(
                            batch = b,
                            checked = selected[b.id] == true,
                            takeText = takeQtyText[b.id] ?: "",
                            onCheckedChange = { on ->
                                selected[b.id] = on
                                if (on && takeQtyText[b.id].isNullOrBlank()) {
                                    takeQtyText[b.id] = formatQty(b.quantity)
                                }
                            },
                            onQtyChange = { takeQtyText[b.id] = it },
                        )
                    }
                    Text(
                        "Total tomado: ${formatQty(totalInput)} postes",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                HorizontalDivider()

                Text("Resultado", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = successText,
                        onValueChange = { successText = it },
                        label = { Text("Exitosos") },
                        modifier = Modifier.weight(1f),
                    )
                    TextField(
                        value = failedText,
                        onValueChange = { failedText = it },
                        label = { Text("Fallados") },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (successValue != null && failedValue != null && !distributedOk) {
                    Text(
                        "Exitosos + fallados (${formatQty(successValue + failedValue)}) " +
                            "debe ser igual al total tomado (${formatQty(totalInput)}).",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                HorizontalDivider()

                Text("Procesamiento", style = MaterialTheme.typography.labelLarge)
                TextField(
                    value = whenText,
                    onValueChange = { whenText = it },
                    label = { Text("Fecha / hora (yyyy-MM-dd HH:mm)") },
                    isError = !whenValid,
                )
                TextField(
                    value = durationText,
                    onValueChange = { durationText = it },
                    label = { Text("Duración total (minutos)") },
                )

                HorizontalDivider()

                Text("Insumos consumidos", style = MaterialTheme.typography.labelLarge)
                if (resources.isEmpty()) {
                    Text(
                        "Cree insumos en la sección Insumos para poder registrar el consumo.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                resourceLines.forEachIndexed { idx, triple ->
                    val (resId, amount, label) = triple
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ResourcePicker(
                                resources = resources,
                                selectedId = resId,
                                onSelect = { sel -> resourceLines[idx] = Triple(sel, amount, label) },
                                modifier = Modifier.weight(1.1f),
                            )
                            TextField(
                                value = amount,
                                onValueChange = { resourceLines[idx] = Triple(resId, it, label) },
                                label = { Text("Cantidad") },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        TextField(
                            value = label,
                            onValueChange = { resourceLines[idx] = Triple(resId, amount, it) },
                            label = { Text("Nota de línea (opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                OutlinedButton(
                    onClick = {
                        val first = resources.firstOrNull()?.id ?: return@OutlinedButton
                        resourceLines.add(Triple(first, "1", ""))
                    },
                    enabled = resources.isNotEmpty(),
                ) {
                    Text("Agregar insumo")
                }

                TextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas generales de la transformación") },
                    modifier = Modifier.fillMaxWidth(),
                )

                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = canSubmit,
                onClick = {
                    val processedAt = parseDateTime(whenText) ?: System.currentTimeMillis()
                    val duration = durationText.toIntOrNull()?.coerceAtLeast(0) ?: 0
                    val inputs =
                        availableBatches.mapNotNull { b ->
                            if (selected[b.id] != true) return@mapNotNull null
                            val q = takeQtyText[b.id]?.toDoubleOrNull() ?: return@mapNotNull null
                            if (q <= 0) null
                            else InventoryRepository.SourceDraft(b.id, q)
                        }
                    val uses =
                        resourceLines.mapNotNull { (rid, amt, lbl) ->
                            val a = amt.toDoubleOrNull()
                            if (a == null || a <= 0) null
                            else InventoryRepository.ResourceUse(rid, a, lbl.trim())
                        }
                    scope.launch {
                        val result =
                            withContext(Dispatchers.IO) {
                                repo.createTransformation(
                                    fromStage = fromStage,
                                    inputs = inputs,
                                    successCount = successValue ?: 0.0,
                                    failedCount = failedValue ?: 0.0,
                                    durationMinutes = duration,
                                    processedAtEpochMs = processedAt,
                                    notes = notes.trim().ifBlank { null },
                                    resourceUses = uses,
                                )
                            }
                        when (result) {
                            is InventoryRepository.TransformationResult.Ok -> onDone()
                            is InventoryRepository.TransformationResult.Err -> errorMsg = result.message
                        }
                    }
                },
            ) {
                Text("Registrar transformación")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
private fun BatchRow(
    batch: Product,
    checked: Boolean,
    takeText: String,
    onCheckedChange: (Boolean) -> Unit,
    onQtyChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Column(modifier = Modifier.weight(1.4f)) {
            Text(batch.name, fontWeight = FontWeight.Medium)
            Text(
                "${batch.productLine} · disponible ${formatQty(batch.quantity)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextField(
            value = takeText,
            onValueChange = onQtyChange,
            label = { Text("Tomar") },
            enabled = checked,
            modifier = Modifier.weight(0.8f),
        )
    }
}

@Composable
private fun ResourcePicker(
    resources: List<Resource>,
    selectedId: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = resources.firstOrNull { it.id == selectedId } ?: resources.firstOrNull()
    OutlinedButton(
        onClick = {
            if (resources.isEmpty()) return@OutlinedButton
            val idx = resources.indexOfFirst { it.id == selectedId }.let { if (it < 0) 0 else it }
            val next = resources[(idx + 1) % resources.size]
            onSelect(next.id)
        },
        modifier = modifier,
    ) {
        Text(current?.let { "${it.name} (${it.unit})" } ?: "Cree insumos primero")
    }
}
