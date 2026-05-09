package com.inventory.industry.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.inventory.industry.data.AcquisitionTransportLineDraft
import com.inventory.industry.data.CatalogProduct
import com.inventory.industry.data.InventoryRepository
import com.inventory.industry.data.PoleInboundEta
import com.inventory.industry.data.PoleProvider
import com.inventory.industry.data.Product
import com.inventory.industry.data.Resource
import com.inventory.industry.data.StageResourceTemplate
import com.inventory.industry.data.Transformation
import com.inventory.industry.data.TransformationProcessingStatus
import com.inventory.industry.domain.PoleStorageLocation
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
    var showStartProcessDialog by remember { mutableStateOf(false) }
    var startPreselectId by remember { mutableStateOf<Int?>(null) }
    var completeTarget by remember { mutableStateOf<Transformation?>(null) }
    var wipInStage by remember { mutableStateOf<List<Transformation>>(emptyList()) }
    var wipActionError by remember { mutableStateOf<String?>(null) }
    var markFailedTarget by remember { mutableStateOf<Product?>(null) }
    var inboundEta by remember { mutableStateOf<Map<Int, PoleInboundEta>>(emptyMap()) }
    val scope = rememberCoroutineScope()

    fun reload() {
        wipActionError = null
        scope.launch {
            products =
                withContext(Dispatchers.IO) {
                    repo.listProducts(stage)
                }
            inboundEta =
                withContext(Dispatchers.IO) {
                    repo.inboundEtaByProductId(products.map { it.id })
                }
            wipInStage =
                withContext(Dispatchers.IO) {
                    repo.listInProgressTransformations(stage)
                }
        }
    }

    LaunchedEffect(stage) {
        reload()
    }

    val availableForProcess =
        remember(products) {
            products.filter {
                !it.isFailed && it.acquisitionStorageLocation == PoleStorageLocation.FABRICA
            }
        }
    val canStartOrRegister =
        remember(availableForProcess) { availableForProcess.isNotEmpty() }
    val awaitingPlantCount =
        remember(products) {
            products.count {
                !it.isFailed && it.acquisitionStorageLocation != PoleStorageLocation.FABRICA
            }
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
                "Primero «Iniciar proceso»: declara el trabajo entre esta etapa y la siguiente. " +
                    "Cuando termine, use «Completar» para registrar exitosos y fallados y mover inventario. " +
                    "«Registrar de una vez» omite el paso intermedio. " +
                    "Solo los lotes en ubicación Fábrica pueden entrar a proceso. " +
                    "El costo de adquisición incluye el pago al proveedor por poste más los traslados (predio y viaje cerrado en «Traslados»), repartidos sobre el lote. " +
                    "Si el material sigue en proveedor o en traslado, use «Traslados» para la ETA y la llegada.",
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                startPreselectId = null
                                showStartProcessDialog = true
                            },
                            enabled = canStartOrRegister,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                            Text(
                                "  Iniciar proceso → ${stage.next()!!.shortCode}",
                            )
                        }
                        OutlinedButton(
                            onClick = { transformTrigger = TransformTrigger(preselectId = null) },
                            enabled = canStartOrRegister,
                        ) {
                            Text("Registrar de una vez")
                        }
                    }
                }
            }

            wipActionError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            if (wipInStage.isNotEmpty()) {
                Text(
                    "Procesos en curso (${stage.shortCode})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                wipInStage.forEach { w ->
                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                    ) {
                        Column(
                            Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                "#${w.id} → ${w.toStage.shortCode} · " +
                                    "${formatQty(w.totalInput)} postes planificados",
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                "Inicio: ${
                                    formatEpochMs(
                                        w.startedAtEpochMs ?: w.processedAtEpochMs,
                                    )
                                }",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            w.notes?.takeIf { it.isNotBlank() }?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { completeTarget = w }) {
                                    Text("Completar (éxitos / fallas)")
                                }
                                TextButton(
                                    onClick = {
                                        wipActionError = null
                                        scope.launch {
                                            val r =
                                                withContext(Dispatchers.IO) {
                                                    repo.cancelStageProcess(w.id)
                                                }
                                            when (r) {
                                                is InventoryRepository.TransformationResult.Ok -> {
                                                    reload()
                                                }
                                                is InventoryRepository.TransformationResult.Err -> {
                                                    wipActionError = r.message
                                                }
                                            }
                                        }
                                    },
                                ) {
                                    Text("Cancelar proceso", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            if (awaitingPlantCount > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                ) {
                    Text(
                        "$awaitingPlantCount lote(s) aún no están en fábrica: la llegada estimada a instalaciones y el cierre del envío se gestionan en «Traslados». " +
                            "Aquí podrá iniciar proceso cuando el lote figure en ubicación Fábrica (tras registrar la llegada).",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }

            ProductTable(
                products = products,
                inboundEta = inboundEta,
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
                    startPreselectId = it.id
                    showStartProcessDialog = true
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
                        val pid =
                            repo.upsertProduct(
                                id = draft.id,
                                name = draft.name,
                                productLine = draft.productLine,
                                stage = draft.stage,
                                quantity = draft.quantity,
                                notes = draft.notes,
                                catalogProductId = draft.catalogProductId,
                                providerId = draft.providerId,
                                standardSalePrice = draft.standardSalePrice,
                                failedSalePrice = draft.failedSalePrice,
                                acquisitionCostPerPole = draft.acquisitionCostPerPole,
                                acquisitionStorageLocation = draft.acquisitionStorageLocation,
                            )
                        repo.syncAcquisitionTransportCosts(
                            productId = pid,
                            location = draft.acquisitionStorageLocation,
                            lines = draft.transportLines,
                        )
                    }
                    showEditor = false
                    reload()
                }
            },
        )
    }

    if (showStartProcessDialog) {
        StartStageProcessDialog(
            repo = repo,
            fromStage = stage,
            availableBatches = availableForProcess,
            preselectId = startPreselectId,
            onDismiss = {
                showStartProcessDialog = false
                startPreselectId = null
            },
            onDone = {
                showStartProcessDialog = false
                startPreselectId = null
                reload()
            },
        )
    }

    transformTrigger?.let { trigger ->
        OneStepTransformationDialog(
            repo = repo,
            fromStage = stage,
            availableBatches = availableForProcess,
            preselectId = trigger.preselectId,
            onDismiss = { transformTrigger = null },
            onDone = {
                transformTrigger = null
                reload()
            },
        )
    }

    completeTarget?.let { tx ->
        CompleteStageProcessDialog(
            repo = repo,
            transformation = tx,
            onDismiss = { completeTarget = null },
            onDone = {
                completeTarget = null
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

private data class TransportLineEditorRow(
    val label: String,
    val costText: String,
)

@Composable
private fun ProductInboundWorkflowLine(
    product: Product,
    eta: PoleInboundEta?,
) {
    if (product.isFailed) return
    val text =
        when (product.acquisitionStorageLocation) {
            PoleStorageLocation.FABRICA -> null
            PoleStorageLocation.EN_PROVEEDOR ->
                "Ubicación: predio proveedor. Los totales de traslado que cargó al registrar el lote (camión, cargador, etc.) " +
                    "forman parte del costo de adquisición, repartidos entre los postes del lote. Para ver ETA a planta y pasar a " +
                    "«En traslado», use «Traslados»; al cerrar el viaje se suman además flete y grúa del envío. " +
                    "Podrá iniciar proceso aquí cuando el lote quede en Fábrica (tras registrar la llegada)."
            PoleStorageLocation.EN_TRANSITO ->
                buildString {
                    append("Ubicación: en camino a planta. ")
                    if (eta != null) {
                        append("Envío #${eta.transportRunId} · ${eta.driverName} · ${eta.vehiclePlate}. ")
                        append("Salida: ${formatEpochMs(eta.departedAtEpochMs)}. ")
                        val arr = eta.expectedArrivalEpochMs
                        if (arr != null) {
                            append("Llegada estimada a instalaciones: ${formatEpochMs(arr)}. ")
                        } else {
                            append("Sin ETA a instalaciones — puede completarla en «Traslados». ")
                        }
                    } else {
                        append("Sin datos de envío vinculados. ")
                    }
                    append(
                        "Los costos de flete y grúa de este viaje se prorratean en el sistema al cierre y suman al costo de adquisición del lote. ",
                    )
                    append(
                        "Disponible para procesar desde esta pantalla después de «Registrar llegada» en «Traslados» (el lote pasará a Fábrica).",
                    )
                }
        }
    if (text == null) return
    Text(
        text = text,
        modifier =
            Modifier
                .padding(start = 4.dp, top = 2.dp, bottom = 4.dp)
                .fillMaxWidth(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

data class ProductDraft(
    val id: Int?,
    val name: String,
    val productLine: String,
    val stage: ProductStage,
    val quantity: Double,
    val notes: String?,
    val catalogProductId: Int?,
    val providerId: Int?,
    val standardSalePrice: Double?,
    val failedSalePrice: Double?,
    val acquisitionCostPerPole: Double?,
    val acquisitionStorageLocation: PoleStorageLocation,
    val transportLines: List<AcquisitionTransportLineDraft>,
)

@Composable
private fun ProductTable(
    products: List<Product>,
    inboundEta: Map<Int, PoleInboundEta>,
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
            TableHeaderCell("Nombre", 0.85f)
            TableHeaderCell("Línea", 0.58f)
            TableHeaderCell("Prov.", 0.48f)
            TableHeaderCell("Ubic.", 0.5f)
            TableHeaderCell("Cant.", 0.3f)
            TableHeaderCell("Precio", 0.48f)
            TableHeaderCell("Estado", 0.58f)
            TableHeaderCell("Notas", 0.75f)
            TableHeaderCell("Acc.", 0.9f)
        }
        HorizontalDivider()
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(products, key = { it.id }) { p ->
                val canAdvanceThis =
                    showAdvance &&
                        !p.isFailed &&
                        p.acquisitionStorageLocation == PoleStorageLocation.FABRICA
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        TableCell(p.name, 0.85f)
                        TableCell(p.productLine, 0.58f)
                        Text(
                            p.providerName ?: "—",
                            modifier = Modifier.weight(0.48f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                        )
                        Text(
                            p.acquisitionStorageLocation.shortLabel,
                            modifier = Modifier.weight(0.5f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                        )
                        TableCell(formatQty(p.quantity), 0.3f)
                        TableCell(
                            p.effectiveSalePrice()?.let { formatMoney(it) } ?: "—",
                            0.48f,
                        )
                        val statusColor =
                            if (p.isFailed) MaterialTheme.colorScheme.error else Color.Unspecified
                        Text(
                            p.statusLabel(),
                            modifier = Modifier.weight(0.58f),
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor,
                            fontWeight = if (p.isFailed) FontWeight.SemiBold else FontWeight.Normal,
                        )
                        TableCell(p.notes.orEmpty(), 0.75f)
                        Row(
                            modifier = Modifier.weight(0.9f),
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
                            if (canAdvanceThis) {
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
                    ProductInboundWorkflowLine(
                        product = p,
                        eta = inboundEta[p.id],
                    )
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
    var providers by remember { mutableStateOf<List<PoleProvider>>(emptyList()) }
    var catalogProductId by remember { mutableStateOf(initial?.catalogProductId) }
    var providerId by remember { mutableStateOf(initial?.providerId) }
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var line by remember { mutableStateOf(initial?.productLine.orEmpty()) }
    var stage by remember { mutableStateOf(initial?.stage ?: defaultStage) }
    var qty by remember { mutableStateOf(initial?.quantity?.toString() ?: "1") }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }
    var standardPrice by remember { mutableStateOf(initial?.standardSalePrice?.toString() ?: "") }
    var failedPrice by remember { mutableStateOf(initial?.failedSalePrice?.toString() ?: "") }
    var acquisitionCost by remember { mutableStateOf(initial?.acquisitionCostPerPole?.toString() ?: "") }
    var persistedTransportTotal by remember { mutableStateOf(0.0) }
    var storageLocation by remember(initial?.id) {
        mutableStateOf(initial?.acquisitionStorageLocation ?: PoleStorageLocation.FABRICA)
    }
    val transportLineRows = remember { mutableStateListOf<TransportLineEditorRow>() }
    val scroll = rememberScrollState()

    LaunchedEffect(Unit) {
        catalog = withContext(Dispatchers.IO) { repo.listCatalogProducts() }
        providers = withContext(Dispatchers.IO) { repo.listPoleProviders() }
    }

    LaunchedEffect(initial?.id) {
        storageLocation = initial?.acquisitionStorageLocation ?: PoleStorageLocation.FABRICA
        persistedTransportTotal =
            if (initial?.id != null) {
                withContext(Dispatchers.IO) {
                    repo.acquisitionTransportTotalForProduct(initial.id)
                }
            } else {
                0.0
            }
        transportLineRows.clear()
        if (initial?.id != null) {
            val lines =
                withContext(Dispatchers.IO) {
                    repo.listAcquisitionTransportForProduct(initial.id)
                }
            if (lines.isEmpty()) {
                transportLineRows.add(TransportLineEditorRow("Camión", ""))
                transportLineRows.add(TransportLineEditorRow("Cargador / excavadora", ""))
            } else {
                lines.forEach { ln ->
                    transportLineRows.add(
                        TransportLineEditorRow(ln.label, ln.lineCost.toString()),
                    )
                }
            }
        } else {
            transportLineRows.add(TransportLineEditorRow("Camión", ""))
            transportLineRows.add(TransportLineEditorRow("Cargador / excavadora", ""))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nuevo lote de postes" else "Editar lote") },
        text = {
            Column(
                modifier =
                    Modifier
                        .heightIn(max = 480.dp)
                        .verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (catalog.isNotEmpty()) {
                    Text("Desde el catálogo", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CatalogPickerRow(
                            catalog = catalog,
                            selectedId = catalogProductId,
                            onSelect = { id, item ->
                                catalogProductId = id
                                if (item != null) {
                                    name = item.name
                                    line = item.productLine
                                }
                            },
                            modifier = Modifier.weight(1f),
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
                if (providers.isNotEmpty()) {
                    Text(
                        if (stage == ProductStage.CRUDO) {
                            "Proveedor (típico en etapa Crudo)"
                        } else {
                            "Proveedor de origen (opcional)"
                        },
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val currentProv = providers.firstOrNull { it.id == providerId }
                        CycleOrDropdownPicker(
                            items = providers,
                            selected = currentProv,
                            onSelected = { providerId = it.id },
                            labelFor = { it.name },
                            placeholder = "Elegir proveedor…",
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { providerId = null }) {
                            Text("Quitar")
                        }
                    }
                } else {
                    Text(
                        "Sin proveedores registrados. Créelos en «Proveedores» para asignar el ingreso.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                Text("Ubicación del lote al registrar la compra", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PoleStorageLocation.entries.forEach { loc ->
                        OutlinedButton(
                            onClick = { storageLocation = loc },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                loc.shortLabel,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight =
                                    if (storageLocation == loc) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
                Text(
                    when (storageLocation) {
                        PoleStorageLocation.FABRICA ->
                            "Fábrica: el material ya está en planta. El costo por poste de materia prima es el campo de abajo; " +
                                "si hubo traslados registrados antes, ya están en el total de líneas de traslado del lote."
                        PoleStorageLocation.EN_PROVEEDOR ->
                            "Proveedor: registro del ingreso en predio. " +
                                "Flujo de ubicación: (1) cargue abajo los totales de traslado conocidos en origen (camión, cargador, comisiones, etc.); " +
                                "el sistema los reparte entre los postes y los suma al pago al proveedor para el costo de adquisición. " +
                                "(2) En «Traslados» inicie un envío: el lote pasa a «En traslado». " +
                                "(3) Al registrar la llegada, el flete y grúa del viaje se suman al mismo costo y el lote pasa a Fábrica."
                        PoleStorageLocation.EN_TRANSITO ->
                            "En traslado: la ubicación la actualiza «Traslados» al iniciar o cerrar un envío. " +
                                "No cambie a Fábrica manualmente: use «Registrar llegada» para imputar flete/grúa y dejar el lote en planta."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextField(
                    value = acquisitionCost,
                    onValueChange = { acquisitionCost = it },
                    label = { Text("Costo materia prima por poste — pago al proveedor (opcional)") },
                )
                if (initial?.id != null) {
                    val qPreview = qty.toDoubleOrNull() ?: 0.0
                    val trPerPole =
                        if (qPreview > 1e-12) persistedTransportTotal / qPreview else 0.0
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "Traslado ya imputado a este lote en el sistema: ${formatMoney(persistedTransportTotal)} " +
                                "(≈ ${formatMoney(trPerPole)} por poste con la cantidad indicada arriba). " +
                                "Esto incluye líneas guardadas en predio y, si correspondió, la parte de flete y grúa de viajes cerrados. " +
                                "Costo puesto en planta por poste ≈ pago proveedor (arriba) + traslado por poste.",
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (storageLocation == PoleStorageLocation.EN_PROVEEDOR) {
                    Text("Costos de traslado (total para todo el lote)", style = MaterialTheme.typography.labelLarge)
                    transportLineRows.forEachIndexed { i, row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextField(
                                value = row.label,
                                onValueChange = { v ->
                                    transportLineRows[i] = row.copy(label = v)
                                },
                                label = { Text("Concepto") },
                                modifier = Modifier.weight(1f),
                            )
                            TextField(
                                value = row.costText,
                                onValueChange = { v ->
                                    transportLineRows[i] = row.copy(costText = v)
                                },
                                label = { Text("Monto") },
                                modifier = Modifier.weight(0.85f),
                            )
                            IconButton(
                                onClick = {
                                    if (transportLineRows.size > 1) transportLineRows.removeAt(i)
                                },
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Quitar línea")
                            }
                        }
                    }
                    TextButton(
                        onClick = {
                            transportLineRows.add(TransportLineEditorRow("Otro", ""))
                        },
                    ) {
                        Text("+ Agregar línea de traslado")
                    }
                    Text(
                        "Cada monto es un total de lote; al guardar se reparte entre los postes y se acumula al costo de adquisición " +
                            "junto al pago al proveedor. Evite duplicar aquí el mismo flete que luego registrará como total de viaje en «Traslados».",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "En transformaciones y ventas, el costo puesto en planta usa materia prima por poste + traslado por poste (suma de las líneas del lote).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                    val transportDrafts: List<AcquisitionTransportLineDraft> =
                        if (storageLocation == PoleStorageLocation.EN_PROVEEDOR) {
                            transportLineRows.mapNotNull { r ->
                                val c = r.costText.toDoubleOrNull() ?: return@mapNotNull null
                                if (c <= 1e-12) return@mapNotNull null
                                AcquisitionTransportLineDraft(
                                    label = r.label.trim().ifBlank { "Traslado" },
                                    lineCost = c,
                                    notes = null,
                                )
                            }
                        } else {
                            emptyList()
                        }
                    onSave(
                        ProductDraft(
                            id = initial?.id,
                            name = name.trim(),
                            productLine = line.trim(),
                            stage = stage,
                            quantity = q,
                            notes = notes.ifBlank { null },
                            catalogProductId = catalogProductId,
                            providerId = providerId,
                            standardSalePrice = standardPrice.toDoubleOrNull(),
                            failedSalePrice = failedPrice.toDoubleOrNull(),
                            acquisitionCostPerPole = acquisitionCost.toDoubleOrNull(),
                            acquisitionStorageLocation = storageLocation,
                            transportLines = transportDrafts,
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
private fun CatalogPickerRow(
    catalog: List<CatalogProduct>,
    selectedId: Int?,
    onSelect: (Int?, CatalogProduct?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (catalog.isEmpty()) return
    val current = catalog.firstOrNull { it.id == selectedId }
    CycleOrDropdownPicker(
        items = catalog,
        selected = current,
        onSelected = { onSelect(it.id, it) },
        labelFor = { "${it.name} · ${it.productLine}" },
        placeholder = "Elegir del catálogo…",
        modifier = modifier,
    )
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
private fun StartStageProcessDialog(
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
    var whenText by remember { mutableStateOf(formatEpochMs(System.currentTimeMillis())) }
    var notes by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

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

    val whenValid = parseDateTime(whenText) != null
    val canStart =
        availableBatches.any { selected[it.id] == true } && totalInput > 0.0 && whenValid
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Iniciar proceso ${fromStage.shortCode} → ${toStage.shortCode}") },
        text = {
            Column(
                modifier =
                    Modifier
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Indica qué lotes entrarán al proceso y cuántos postes. " +
                        "El inventario no cambia hasta que registre el resultado en «Completar».",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (availableBatches.isEmpty()) {
                    Text(
                        "No hay lotes disponibles en ${fromStage.shortCode}.",
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Text("Lotes que entran al proceso", style = MaterialTheme.typography.labelLarge)
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
                        "Total planificado: ${formatQty(totalInput)} postes",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                TextField(
                    value = whenText,
                    onValueChange = { whenText = it },
                    label = { Text("Inicio del proceso (yyyy-MM-dd HH:mm)") },
                    isError = !whenValid,
                )
                TextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                errorMsg?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val startedAt = parseDateTime(whenText) ?: System.currentTimeMillis()
                    val inputs =
                        availableBatches.mapNotNull { b ->
                            if (selected[b.id] != true) return@mapNotNull null
                            val q = takeQtyText[b.id]?.toDoubleOrNull() ?: return@mapNotNull null
                            if (q <= 0) null
                            else InventoryRepository.SourceDraft(b.id, q)
                        }
                    scope.launch {
                        val result =
                            withContext(Dispatchers.IO) {
                                repo.startStageProcess(
                                    fromStage = fromStage,
                                    inputs = inputs,
                                    startedAtEpochMs = startedAt,
                                    notes = notes.trim().ifBlank { null },
                                )
                            }
                        when (result) {
                            is InventoryRepository.TransformationResult.Ok -> onDone()
                            is InventoryRepository.TransformationResult.Err -> errorMsg = result.message
                        }
                    }
                },
                enabled = canStart,
            ) {
                Text("Iniciar proceso")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
private fun CompleteStageProcessDialog(
    repo: InventoryRepository,
    transformation: Transformation,
    onDismiss: () -> Unit,
    onDone: () -> Unit,
) {
    if (transformation.processingStatus != TransformationProcessingStatus.IN_PROGRESS) return

    val fromStage = transformation.fromStage
    val toStage = transformation.toStage
    val planned = transformation.totalInput

    var successText by remember { mutableStateOf("") }
    var failedText by remember { mutableStateOf("0") }
    var whenText by remember { mutableStateOf(formatEpochMs(System.currentTimeMillis())) }
    var durationText by remember { mutableStateOf("60") }
    var completeNotes by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(planned) {
        successText = formatQty(planned)
        failedText = "0"
    }

    var resources by remember { mutableStateOf<List<Resource>>(emptyList()) }
    var stageTemplates by remember { mutableStateOf<List<StageResourceTemplate>>(emptyList()) }
    val resourceLines = remember { mutableStateListOf<Triple<Int, String, String>>() }
    LaunchedEffect(Unit) {
        resources = withContext(Dispatchers.IO) { repo.listResources() }
    }
    LaunchedEffect(fromStage) {
        stageTemplates = withContext(Dispatchers.IO) { repo.listStageResourceTemplates(fromStage) }
    }

    val scope = rememberCoroutineScope()
    val successValue = successText.toDoubleOrNull()
    val failedValue = failedText.toDoubleOrNull()
    val distributedOk =
        successValue != null && failedValue != null &&
            kotlin.math.abs((successValue + failedValue) - planned) < 1e-6
    val whenValid = parseDateTime(whenText) != null
    val canSubmit =
        planned > 0.0 &&
            distributedOk &&
            whenValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar resultado · #${transformation.id} → ${toStage.shortCode}") },
        text = {
            Column(
                modifier =
                    Modifier
                        .heightIn(max = 560.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Planeado: ${formatQty(planned)} postes desde ${fromStage.shortCode}. " +
                        "Indique cuántos pasan a ${toStage.shortCode} y cuántos fallan en origen.",
                    style = MaterialTheme.typography.bodySmall,
                )
                transformation.inputs.forEach { inp ->
                    Text(
                        "· ${inp.sourceName} — ${formatQty(inp.quantity)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                HorizontalDivider()

                Text("Resultado", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = successText,
                        onValueChange = { successText = it },
                        label = { Text("Exitosos (siguiente etapa)") },
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
                            "debe ser igual al planeado (${formatQty(planned)}).",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                HorizontalDivider()

                Text("Procesamiento", style = MaterialTheme.typography.labelLarge)
                TextField(
                    value = whenText,
                    onValueChange = { whenText = it },
                    label = { Text("Fecha / hora cierre (yyyy-MM-dd HH:mm)") },
                    isError = !whenValid,
                )
                TextField(
                    value = durationText,
                    onValueChange = { durationText = it },
                    label = { Text("Duración total (minutos)") },
                )

                HorizontalDivider()

                Text("Insumos consumidos", style = MaterialTheme.typography.labelLarge)
                if (stageTemplates.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                resourceLines.clear()
                                val uses =
                                    repo.suggestResourceUsesFromRecipe(fromStage, planned)
                                uses.forEach { u ->
                                    resourceLines.add(
                                        Triple(
                                            u.resourceId,
                                            formatQty(u.amount),
                                            u.label,
                                        ),
                                    )
                                }
                            },
                            enabled = planned > 0 && resources.isNotEmpty(),
                        ) {
                            Text("Aplicar receta (${formatQty(planned)} postes)")
                        }
                        OutlinedButton(
                            onClick = { resourceLines.clear() },
                            enabled = resourceLines.isNotEmpty(),
                        ) {
                            Text("Vaciar")
                        }
                    }
                }
                resourceLines.forEachIndexed { idx, triple ->
                    val (resId, amount, label) = triple
                    val resMeta = resources.firstOrNull { it.id == resId }
                    val tpl = stageTemplates.firstOrNull { it.resourceId == resId }
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
                                label = { Text("Cantidad usada") },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        TextField(
                            value = label,
                            onValueChange = { resourceLines[idx] = Triple(resId, amount, it) },
                            label = { Text("Nota de línea (opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (tpl != null && resMeta != null && planned > 0) {
                            Text(
                                "Receta: ${formatQty(tpl.amountPerPole)} ${resMeta.unit}/poste × " +
                                    "${formatQty(planned)} = ${formatQty(tpl.amountPerPole * planned)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
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
                    value = completeNotes,
                    onValueChange = { completeNotes = it },
                    label = { Text("Notas al cerrar (opcional)") },
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
                    val uses =
                        resourceLines.mapNotNull { (rid, amt, lbl) ->
                            val a = amt.toDoubleOrNull()
                            if (a == null || a <= 0) null
                            else InventoryRepository.ResourceUse(rid, a, lbl.trim())
                        }
                    scope.launch {
                        val result =
                            withContext(Dispatchers.IO) {
                                repo.completeStageProcess(
                                    transformationId = transformation.id,
                                    successCount = successValue ?: 0.0,
                                    failedCount = failedValue ?: 0.0,
                                    durationMinutes = duration,
                                    processedAtEpochMs = processedAt,
                                    completeNotes = completeNotes.trim().ifBlank { null },
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
                Text("Registrar resultado")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Volver") }
        },
    )
}

@Composable
private fun OneStepTransformationDialog(
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
    var stageTemplates by remember { mutableStateOf<List<StageResourceTemplate>>(emptyList()) }
    val resourceLines = remember { mutableStateListOf<Triple<Int, String, String>>() }
    LaunchedEffect(Unit) {
        resources = withContext(Dispatchers.IO) { repo.listResources() }
    }
    LaunchedEffect(fromStage) {
        stageTemplates = withContext(Dispatchers.IO) { repo.listStageResourceTemplates(fromStage) }
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
            Column(
                modifier =
                    Modifier
                        .heightIn(max = 560.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
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
                if (stageTemplates.isNotEmpty()) {
                    Text(
                        "${stageTemplates.size} líneas en la receta de ${fromStage.shortCode}. " +
                            "Aplique para multiplicar por el total de postes; luego puede ajustar cantidades.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                resourceLines.clear()
                                val uses =
                                    repo.suggestResourceUsesFromRecipe(fromStage, totalInput)
                                uses.forEach { u ->
                                    resourceLines.add(
                                        Triple(
                                            u.resourceId,
                                            formatQty(u.amount),
                                            u.label,
                                        ),
                                    )
                                }
                            },
                            enabled = totalInput > 0 && resources.isNotEmpty(),
                        ) {
                            Text("Aplicar receta (${formatQty(totalInput)} postes)")
                        }
                        OutlinedButton(
                            onClick = { resourceLines.clear() },
                            enabled = resourceLines.isNotEmpty(),
                        ) {
                            Text("Vaciar")
                        }
                    }
                } else {
                    Text(
                        "Sin receta para ${fromStage.shortCode}. Configúrela en «Recetas» o agregue insumos a mano.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (resources.isEmpty()) {
                    Text(
                        "Cree insumos en la sección Insumos para poder registrar el consumo.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                resourceLines.forEachIndexed { idx, triple ->
                    val (resId, amount, label) = triple
                    val resMeta = resources.firstOrNull { it.id == resId }
                    val tpl = stageTemplates.firstOrNull { it.resourceId == resId }
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
                                label = { Text("Cantidad usada") },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        TextField(
                            value = label,
                            onValueChange = { resourceLines[idx] = Triple(resId, amount, it) },
                            label = { Text("Nota de línea (opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (tpl != null && resMeta != null && totalInput > 0) {
                            Text(
                                "Receta: ${formatQty(tpl.amountPerPole)} ${resMeta.unit}/poste × " +
                                    "${formatQty(totalInput)} = ${formatQty(tpl.amountPerPole * totalInput)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
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
    CycleOrDropdownPicker(
        items = resources,
        selected = current,
        onSelected = { onSelect(it.id) },
        labelFor = { "${it.name} (${it.unit})" },
        placeholder = "Cree insumos primero",
        modifier = modifier,
        enabled = resources.isNotEmpty(),
    )
}
