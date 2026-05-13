package com.inventory.industry.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.inventory.industry.ui.app.LocalAppMessenger
import com.inventory.industry.ui.components.buttons.AppButton
import com.inventory.industry.ui.components.buttons.AppFloatingActionButton
import com.inventory.industry.ui.components.buttons.AppIconButton
import com.inventory.industry.ui.components.buttons.AppOutlinedButton
import com.inventory.industry.ui.components.cards.AppCard
import com.inventory.industry.ui.components.feedback.StatusChip
import com.inventory.industry.ui.components.inputs.AppDropdownField
import com.inventory.industry.ui.components.inputs.AppSearchField
import com.inventory.industry.ui.layout.SectionContainer
import com.inventory.industry.ui.models.StatusKind
import com.inventory.industry.ui.modifiers.smoothClickable
import com.inventory.industry.ui.modifiers.trackHover
import com.inventory.industry.ui.navigation.PageHeader
import com.inventory.industry.ui.theme.AppShapes
import com.inventory.industry.ui.theme.AppSpacing
import com.inventory.industry.ui.theme.AppTypography
import androidx.compose.ui.graphics.Brush
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

/** Minimum table width; actual width grows with the viewport up to full content area. */
private val PostesTableMinWidth = 1080.dp

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
    var searchQuery by remember { mutableStateOf("") }
    var lineFilter by remember { mutableStateOf("") }
    var tablePage by remember { mutableStateOf(0) }
    var rowsPerPage by remember { mutableStateOf(25) }
    var showHelp by remember { mutableStateOf(false) }
    var compactToolbar by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val messenger = LocalAppMessenger.current

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

    LaunchedEffect(stage, searchQuery, lineFilter, rowsPerPage) {
        tablePage = 0
    }

    val filteredProducts by remember {
        derivedStateOf {
            val q = searchQuery.trim().lowercase()
            products.filter { p ->
                val lineOk = lineFilter.isBlank() || p.productLine == lineFilter
                if (!lineOk) return@filter false
                if (q.isEmpty()) return@filter true
                p.name.lowercase().contains(q) ||
                    p.productLine.lowercase().contains(q) ||
                    (p.providerName?.lowercase()?.contains(q) == true) ||
                    p.acquisitionStorageLocation.shortLabel.lowercase().contains(q) ||
                    (p.notes?.lowercase()?.contains(q) == true)
            }
        }
    }

    val pageCount =
        remember(filteredProducts.size, rowsPerPage) {
            val n = filteredProducts.size
            if (n == 0) 1 else (n + rowsPerPage - 1) / rowsPerPage
        }
    LaunchedEffect(tablePage, pageCount) {
        if (tablePage >= pageCount) {
            tablePage = (pageCount - 1).coerceAtLeast(0)
        }
    }

    val pagedProducts =
        remember(filteredProducts, tablePage, rowsPerPage) {
            filteredProducts.drop(tablePage * rowsPerPage).take(rowsPerPage)
        }

    val lineChoices =
        remember(products) {
            listOf("Todas las líneas") + products.map { it.productLine }.distinct().sorted()
        }
    val lineDropdownSelected =
        if (lineFilter.isBlank()) {
            "Todas las líneas"
        } else {
            lineFilter
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

    val fabInteraction = remember { MutableInteractionSource() }
    val fabHovered by fabInteraction.collectIsHoveredAsState()
    val fabScale by animateFloatAsState(if (fabHovered) 1.06f else 1f, tween(160), label = "fabScale")
    val primaryGradient =
        Brush.horizontalGradient(
            listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
            ),
        )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
        ) {
            PageHeader(
                title = "Postes por etapa",
                subtitle =
                    "Flujo de lotes por etapa productiva. Inicie proceso, complete transformaciones o registre de una vez. " +
                        "Solo lotes en Fábrica pueden avanzar.",
                actions = {
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                        IconButton(onClick = { showHelp = true }) {
                            Icon(
                                Icons.Outlined.HelpOutline,
                                contentDescription = "Ayuda",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { compactToolbar = !compactToolbar }) {
                            Icon(
                                Icons.Outlined.Settings,
                                contentDescription = "Vista compacta de filtros",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )

            SectionContainer(horizontalPadding = 0.dp, verticalPadding = 0.dp) {
                WorkflowStageTabRow(
                    selectedIndex = tab,
                    onSelect = { tab = it },
                )
            }

            AppCard(
                modifier = Modifier.fillMaxWidth().weight(1f),
                enableHoverElevation = false,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(AppSpacing.lg),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                ) {
                    Text(
                        text = stage.title,
                        style = AppTypography.SectionTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    if (stage.next() != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppButton(
                                text = "Iniciar proceso → ${stage.next()!!.shortCode}",
                                onClick = {
                                    startPreselectId = null
                                    showStartProcessDialog = true
                                },
                                enabled = canStartOrRegister,
                                gradient = primaryGradient,
                                leadingIcon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                    )
                                },
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            AppOutlinedButton(
                                text = "Registrar de una vez",
                                onClick = { transformTrigger = TransformTrigger(preselectId = null) },
                                enabled = canStartOrRegister,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        }
                    }

                    wipActionError?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = AppTypography.BodySmall,
                        )
                    }

                    if (wipInStage.isNotEmpty()) {
                        Text(
                            "Procesos en curso (${stage.shortCode})",
                            style = AppTypography.CardTitle,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        wipInStage.forEach { w ->
                            AppCard(
                                modifier = Modifier.fillMaxWidth(),
                                enableHoverElevation = true,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(AppSpacing.md),
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                                    StatusChip(
                                        text = "En proceso · #${w.id} → ${w.toStage.shortCode}",
                                        kind = StatusKind.Info,
                                    )
                                    Text(
                                        "${formatQty(w.totalInput)} postes planificados · Inicio: " +
                                            formatEpochMs(w.startedAtEpochMs ?: w.processedAtEpochMs),
                                        style = AppTypography.BodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    w.notes?.takeIf { it.isNotBlank() }?.let { n ->
                                        Text(n, style = AppTypography.Caption)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                                        AppButton(
                                            text = "Completar (éxitos / fallas)",
                                            onClick = { completeTarget = w },
                                            modifier = Modifier.weight(1f, fill = false),
                                        )
                                        TextButton(
                                            onClick = {
                                                wipActionError = null
                                                scope.launch {
                                                    val r =
                                                        withContext(Dispatchers.IO) {
                                                            repo.cancelStageProcess(w.id)
                                                        }
                                                    when (r) {
                                                        is InventoryRepository.TransformationResult.Ok -> reload()
                                                        is InventoryRepository.TransformationResult.Err ->
                                                            wipActionError = r.message
                                                    }
                                                }
                                            },
                                        ) {
                                            Text("Cancelar", color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (awaitingPlantCount > 0) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(AppShapes.medium)
                                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f))
                                    .padding(AppSpacing.md),
                        ) {
                            Text(
                                "$awaitingPlantCount lote(s) aún no están en fábrica. Use «Traslados» para ETA y llegada; " +
                                    "luego podrá iniciar proceso aquí.",
                                style = AppTypography.BodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }

                    AnimatedVisibility(visible = !compactToolbar) {
                        PostesInventoryFilterToolbar(
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            lineChoices = lineChoices,
                            lineSelected = lineDropdownSelected,
                            onLineSelected = { choice ->
                                lineFilter = if (choice == "Todas las líneas") "" else choice
                            },
                            onExport = {
                                val csv = postesInventoryExportCsv(filteredProducts)
                                clipboard.setText(AnnotatedString(csv))
                                messenger.showSuccess("Exportación copiada al portapapeles (${filteredProducts.size} filas).")
                            },
                            onSettingsToggle = { compactToolbar = true },
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    EnterpriseInventoryTable(
                        products = pagedProducts,
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
                        onMarkFailed = { markFailedTarget = it },
                        onClearFailure = {
                            scope.launch {
                                withContext(Dispatchers.IO) { repo.clearProductFailure(it.id) }
                                reload()
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )

                    InventoryTablePaginationFooter(
                        page = tablePage,
                        pageCount = pageCount,
                        rowsPerPage = rowsPerPage,
                        onRowsPerPageChange = { rowsPerPage = it },
                        totalItems = filteredProducts.size,
                        onPrev = { tablePage = (tablePage - 1).coerceAtLeast(0) },
                        onNext = { tablePage = (tablePage + 1).coerceAtMost(pageCount - 1) },
                    )
                }
            }
        }

        AppFloatingActionButton(
            onClick = {
                editing = null
                showEditor = true
            },
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(AppSpacing.xxl)
                    .scale(fabScale),
            interactionSource = fabInteraction,
            gradient = primaryGradient,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            size = 60.dp,
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nuevo lote", tint = MaterialTheme.colorScheme.onPrimary)
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("Cómo usar esta vista", style = AppTypography.SectionTitle) },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                ) {
                    Text(
                        "«Iniciar proceso» declara el trabajo entre esta etapa y la siguiente. Al terminar, use «Completar» " +
                            "para registrar exitosos y fallados y mover inventario.",
                        style = AppTypography.BodySmall,
                    )
                    Text(
                        "«Registrar de una vez» omite el paso intermedio. Solo los lotes en ubicación Fábrica pueden entrar a proceso.",
                        style = AppTypography.BodySmall,
                    )
                    Text(
                        "El costo de adquisición incluye el pago al proveedor por poste más los traslados " +
                            "(predio y viaje en «Traslados»), repartidos sobre el lote.",
                        style = AppTypography.BodySmall,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) { Text("Cerrar") }
            },
        )
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

private fun csvField(raw: String): String {
    val needQuote = raw.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
    val escaped = raw.replace("\"", "\"\"")
    return if (needQuote) "\"$escaped\"" else escaped
}

private fun postesInventoryExportCsv(products: List<Product>): String =
    buildString {
        appendLine("Nombre,Línea,Prov.,Ubic.,Cant.,Precio,Estado,Notas")
        products.forEach { p ->
            appendLine(
                listOf(
                        csvField(p.name),
                        csvField(p.productLine),
                        csvField(p.providerName.orEmpty()),
                        csvField(p.acquisitionStorageLocation.shortLabel),
                        csvField(formatQty(p.quantity)),
                        csvField(p.effectiveSalePrice()?.let { formatMoney(it) }.orEmpty()),
                        csvField(p.statusLabel()),
                        csvField(p.notes.orEmpty()),
                    )
                    .joinToString(","),
            )
        }
    }

@Composable
private fun WorkflowStageTabRow(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val stages = ProductStage.entries
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm)
                .animateContentSize(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        stages.forEachIndexed { index, stage ->
            WorkflowStageTab(
                stage = stage,
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun workflowStageAccent(stage: ProductStage): Color =
    when (stage) {
        ProductStage.CRUDO -> Color(0xFF7C3AED)
        ProductStage.DESCORTEZADO -> Color(0xFF16A34A)
        ProductStage.TRATADO -> Color(0xFF2563EB)
        ProductStage.TERMINADO -> Color(0xFFEA580C)
    }

private fun workflowStageIcon(stage: ProductStage): ImageVector =
    when (stage) {
        ProductStage.CRUDO -> Icons.Outlined.Layers
        ProductStage.DESCORTEZADO -> Icons.Outlined.ContentCut
        ProductStage.TRATADO -> Icons.Outlined.Science
        ProductStage.TERMINADO -> Icons.Default.CheckCircle
    }

@Composable
private fun WorkflowStageTab(
    stage: ProductStage,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = workflowStageAccent(stage)
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val surfaceAlpha by
        animateFloatAsState(
            targetValue =
                when {
                    selected -> 0.14f
                    hovered -> 0.09f
                    else -> 0f
                },
            animationSpec = tween(180),
            label = "tabSurface",
        )
    val labelColor =
        if (selected) {
            accent
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = if (hovered) 0.92f else 0.72f)
        }
    val indicatorH by animateDpAsState(if (selected) 3.dp else 0.dp, tween(220), label = "tabInd")
    Column(
        modifier =
            modifier
                .clip(AppShapes.medium)
                .background(accent.copy(alpha = surfaceAlpha))
                .smoothClickable(interactionSource = interaction, onClick = onClick)
                .padding(vertical = AppSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            workflowStageIcon(stage),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stage.shortCode,
            style = AppTypography.ButtonText,
            color = labelColor,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.md)
                    .height(3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .fillMaxWidth(0.58f)
                    .height(indicatorH)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent),
            )
        }
    }
}

@Composable
private fun PostesInventoryFilterToolbar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    lineChoices: List<String>,
    lineSelected: String,
    onLineSelected: (String) -> Unit,
    onExport: () -> Unit,
    onSettingsToggle: () -> Unit,
) {
    val scroll = rememberScrollState()
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(scroll),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            AppSearchField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = "Buscar nombre, línea, proveedor, ubicación…",
                modifier = Modifier.widthIn(min = 240.dp, max = 480.dp),
            )
            AppDropdownField(
                label = "Línea",
                options = lineChoices,
                selected = lineSelected,
                onSelected = onLineSelected,
                modifier = Modifier.widthIn(min = 168.dp, max = 240.dp),
                placeholder = "Línea…",
            )
            AppOutlinedButton(
                text = "Exportar",
                onClick = onExport,
                leadingIcon = {
                    Icon(
                        Icons.Outlined.FileDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
            )
            AppIconButton(onClick = onSettingsToggle, size = 44.dp) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = "Vista compacta de filtros",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun RowScope.EnterpriseTableHeaderCell(
    text: String,
    weight: Float,
) {
    Text(
        text,
        modifier = Modifier.weight(weight),
        style = AppTypography.Caption,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun RowScope.EnterpriseTableDataCell(
    text: String,
    weight: Float,
    maxLines: Int = 1,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Text(
        text,
        modifier = Modifier.weight(weight),
        style = AppTypography.BodySmall,
        fontWeight = fontWeight,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun EnterpriseInventoryTable(
    products: List<Product>,
    inboundEta: Map<Int, PoleInboundEta>,
    showAdvance: Boolean,
    onEdit: (Product) -> Unit,
    onDelete: (Product) -> Unit,
    onAdvance: (Product) -> Unit,
    onMarkFailed: (Product) -> Unit,
    onClearFailure: (Product) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val headerBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val hScroll = rememberScrollState()
    BoxWithConstraints(
        modifier =
            modifier
                .clip(AppShapes.large)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    shape = AppShapes.large,
                ),
    ) {
        val viewport =
            if (maxWidth.value.isFinite() && maxWidth >= 0.dp) {
                maxWidth
            } else {
                PostesTableMinWidth
            }
        val tableWidth = maxOf(PostesTableMinWidth, viewport)
        Box(Modifier.fillMaxSize().horizontalScroll(hScroll)) {
            Column(
                modifier =
                    Modifier
                        .width(tableWidth)
                        .fillMaxHeight(),
            ) {
                Column {
                    Row(
                        modifier =
                            Modifier
                                .width(tableWidth)
                                .background(headerBg)
                                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    ) {
                        EnterpriseTableHeaderCell("Nombre", 1.1f)
                        EnterpriseTableHeaderCell("Línea", 0.65f)
                        EnterpriseTableHeaderCell("Prov.", 0.55f)
                        EnterpriseTableHeaderCell("Ubic.", 0.55f)
                        EnterpriseTableHeaderCell("Cant.", 0.35f)
                        EnterpriseTableHeaderCell("Precio", 0.5f)
                        EnterpriseTableHeaderCell("Estado", 0.65f)
                        EnterpriseTableHeaderCell("Notas", 0.95f)
                        EnterpriseTableHeaderCell("Acciones", 1.15f)
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                    )
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                ) {
                    itemsIndexed(products, key = { _, p -> p.id }) { index, p ->
                        EnterpriseInventoryTableRow(
                            index = index,
                            product = p,
                            inboundEta = inboundEta[p.id],
                            showAdvance = showAdvance,
                            tableWidth = tableWidth,
                            onEdit = { onEdit(p) },
                            onDelete = { onDelete(p) },
                            onAdvance = { onAdvance(p) },
                            onMarkFailed = { onMarkFailed(p) },
                            onClearFailure = { onClearFailure(p) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnterpriseInventoryTableRow(
    index: Int,
    product: Product,
    inboundEta: PoleInboundEta?,
    showAdvance: Boolean,
    tableWidth: Dp,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAdvance: () -> Unit,
    onMarkFailed: () -> Unit,
    onClearFailure: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val zebra =
        if (index % 2 == 1) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    val hover = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    val rowBg by
        animateColorAsState(
            targetValue = if (hovered) hover else zebra,
            animationSpec = tween(140),
            label = "invRowBg",
        )
    val canAdvance =
        showAdvance &&
            !product.isFailed &&
            product.acquisitionStorageLocation == PoleStorageLocation.FABRICA
    Column(
        modifier =
            Modifier
                .width(tableWidth)
                .background(rowBg)
                .trackHover(interaction)
                .animateContentSize(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            EnterpriseTableDataCell(
                text = product.name,
                weight = 1.1f,
                fontWeight = FontWeight.Medium,
            )
            EnterpriseTableDataCell(product.productLine, 0.65f)
            EnterpriseTableDataCell(product.providerName ?: "—", 0.55f)
            EnterpriseTableDataCell(
                text = product.acquisitionStorageLocation.shortLabel,
                weight = 0.55f,
                maxLines = 2,
            )
            EnterpriseTableDataCell(formatQty(product.quantity), 0.35f)
            EnterpriseTableDataCell(
                text = product.effectiveSalePrice()?.let { formatMoney(it) } ?: "—",
                weight = 0.5f,
            )
            Box(Modifier.weight(0.65f), contentAlignment = Alignment.CenterStart) {
                val chipText: String
                val chipKind: StatusKind
                if (product.isFailed) {
                    chipText =
                        product.failedAtStage?.shortCode?.let { "Fallado · $it" }
                            ?: "Fallado"
                    chipKind = StatusKind.Error
                } else {
                    chipText = "OK"
                    chipKind = StatusKind.Success
                }
                StatusChip(text = chipText, kind = chipKind)
            }
            EnterpriseTableDataCell(
                text = product.notes.orEmpty(),
                weight = 0.95f,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.weight(1.15f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIconButton(onClick = onEdit, size = 38.dp) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                if (product.stage != ProductStage.TERMINADO && !product.isFailed) {
                    AppIconButton(
                        onClick = onMarkFailed,
                        size = 38.dp,
                        containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                        hoveredColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f),
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Marcar fallado",
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
                if (product.isFailed) {
                    AppIconButton(
                        onClick = onClearFailure,
                        size = 38.dp,
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Quitar falla",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                if (canAdvance) {
                    AppIconButton(
                        onClick = onAdvance,
                        size = 38.dp,
                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                        hoveredColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Mover etapa",
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
                AppIconButton(
                    onClick = onDelete,
                    size = 38.dp,
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    hoveredColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        ProductInboundWorkflowLine(product = product, eta = inboundEta)
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        )
    }
}

@Composable
private fun InventoryTablePaginationFooter(
    page: Int,
    pageCount: Int,
    rowsPerPage: Int,
    onRowsPerPageChange: (Int) -> Unit,
    totalItems: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    val pageSizes = listOf(10, 25, 50, 100)
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text =
                "$totalItems resultado" + if (totalItems != 1) "s" else "",
            style = AppTypography.BodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                Text(
                    "Filas / pág.",
                    style = AppTypography.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AppDropdownField(
                    label = null,
                    options = pageSizes,
                    selected = rowsPerPage,
                    onSelected = onRowsPerPageChange,
                    optionLabel = { "$it" },
                    modifier = Modifier.widthIn(min = 72.dp, max = 96.dp),
                )
            }
            Text(
                text = "${page + 1} / $pageCount",
                style = AppTypography.BodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AppIconButton(onClick = onPrev, enabled = page > 0, size = 40.dp) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Página anterior",
                    tint =
                        if (page > 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                        },
                )
            }
            AppIconButton(onClick = onNext, enabled = page < pageCount - 1, size = 40.dp) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Página siguiente",
                    tint =
                        if (page < pageCount - 1) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                        },
                )
            }
        }
    }
}

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
