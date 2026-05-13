package com.inventory.industry.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inventory.industry.data.Client
import com.inventory.industry.data.InventoryRepository
import com.inventory.industry.data.Product
import com.inventory.industry.data.SaleCostPreview
import com.inventory.industry.data.SaleRecord
import com.inventory.industry.ui.components.buttons.AppButton
import com.inventory.industry.ui.components.buttons.AppFloatingActionButton
import com.inventory.industry.ui.components.buttons.AppOutlinedButton
import com.inventory.industry.ui.components.cards.AppCard
import com.inventory.industry.ui.components.inputs.AppDropdownField
import com.inventory.industry.ui.components.inputs.AppNumberField
import com.inventory.industry.ui.components.inputs.AppTextArea
import com.inventory.industry.ui.components.inputs.AppTextField
import com.inventory.industry.ui.components.sales.SaleHistoryCard
import com.inventory.industry.ui.layout.SectionContainer
import com.inventory.industry.ui.layout.WindowSize
import com.inventory.industry.ui.layout.WindowWidthClass
import com.inventory.industry.ui.navigation.BreadcrumbSegment
import com.inventory.industry.ui.navigation.PageHeader
import com.inventory.industry.ui.navigation.ScreenRoute
import com.inventory.industry.ui.theme.AppShapes
import com.inventory.industry.ui.theme.AppSpacing
import com.inventory.industry.ui.theme.AppThemeState
import com.inventory.industry.ui.theme.AppTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SalesScreen(
    repo: InventoryRepository,
    onNavigate: (ScreenRoute) -> Unit = {},
) {
    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var sellable by remember { mutableStateOf<List<Product>>(emptyList()) }
    var recent by remember { mutableStateOf<List<SaleRecord>>(emptyList()) }
    var clientId by remember { mutableStateOf<Int?>(null) }
    var productId by remember { mutableStateOf<Int?>(null) }
    var qtyText by remember { mutableStateOf("1") }
    var totalText by remember { mutableStateOf("") }
    var marginText by remember { mutableStateOf("20") }
    var whenText by remember { mutableStateOf(formatEpochMs(System.currentTimeMillis())) }
    var saleNotes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var costPreview by remember { mutableStateOf<SaleCostPreview?>(null) }
    var showSaleConfirmDialog by remember { mutableStateOf(false) }
    var formResetNonce by remember { mutableStateOf(0) }
    var suggestLoading by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    fun reloadLists() {
        scope.launch {
            clients = withContext(Dispatchers.IO) { repo.listClients() }
            sellable = withContext(Dispatchers.IO) { repo.listSellableProducts() }
            recent = withContext(Dispatchers.IO) { repo.listSales(80) }
        }
    }

    LaunchedEffect(Unit) {
        reloadLists()
    }

    LaunchedEffect(clients) {
        if (clientId == null && clients.isNotEmpty()) clientId = clients.first().id
    }

    LaunchedEffect(sellable) {
        if (productId == null && sellable.isNotEmpty()) productId = sellable.first().id
    }

    LaunchedEffect(sellable, productId) {
        if (productId != null && sellable.none { it.id == productId }) {
            productId = sellable.firstOrNull()?.id
        }
    }

    val selectedClient = clients.firstOrNull { it.id == clientId }
    val selectedProduct = sellable.firstOrNull { it.id == productId }

    LaunchedEffect(selectedProduct?.id, formResetNonce) {
        val p = selectedProduct ?: return@LaunchedEffect
        qtyText = formatQty(p.quantity)
        val hint = p.effectiveSalePrice()
        totalText =
            if (hint != null) {
                formatMoney(hint * p.quantity)
            } else {
                ""
            }
    }

    LaunchedEffect(selectedProduct?.id, qtyText, marginText) {
        val p = selectedProduct
        val q = qtyText.toDoubleOrNull()
        val m = parseMarginPercent(marginText)
        if (p == null || q == null || m == null || q <= 1e-12) {
            costPreview = null
            return@LaunchedEffect
        }
        costPreview =
            withContext(Dispatchers.IO) {
                repo.saleCostPreview(productId = p.id, quantitySold = q, marginPercent = m)
            }
    }

    val qtyParsed = qtyText.toDoubleOrNull()
    val totalParsed = parseMoneyAmount(totalText)
    val whenParsed = parseDateTime(whenText)
    val marginParsed = parseMarginPercent(marginText)
    val canSubmitSale =
        clients.isNotEmpty() &&
            sellable.isNotEmpty() &&
            clientId != null &&
            clients.any { it.id == clientId } &&
            productId != null &&
            sellable.any { it.id == productId } &&
            qtyParsed != null &&
            qtyParsed > 1e-12 &&
            selectedProduct != null &&
            qtyParsed <= selectedProduct.quantity + 1e-9 &&
            totalParsed != null &&
            totalParsed >= 0 &&
            whenParsed != null &&
            marginParsed != null

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val windowSize =
            remember(maxWidth, maxHeight) {
                WindowSize.fromSize(maxWidth, maxHeight)
            }
        val scheme = MaterialTheme.colorScheme
        val fabGradient =
            Brush.verticalGradient(
                colors =
                    listOf(
                        scheme.primary,
                        scheme.primary.copy(alpha = 0.88f),
                        scheme.tertiary.copy(alpha = 0.92f),
                    ),
            )
        val confirmGradient =
            Brush.horizontalGradient(
                colors =
                    listOf(
                        scheme.primary,
                        scheme.primary.copy(alpha = 0.82f),
                        scheme.tertiary,
                    ),
            )

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = AppSpacing.lg),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
            ) {
                item {
                    PageHeader(
                        title = "Registrar venta",
                        subtitle =
                            "Registre ventas de postes en Terminado (OK) o lotes fallados a precio de saldo. " +
                                "El precio sugerido aplica el % de ganancia sobre costo por poste " +
                                "(material, traslado prorrateado y procesamiento).",
                        breadcrumbs =
                            listOf(
                                BreadcrumbSegment(
                                    label = "Ventas",
                                    onClick = { onNavigate(ScreenRoute.Sales) },
                                ),
                                BreadcrumbSegment(label = "Registrar venta"),
                            ),
                        heroIcon = Icons.Outlined.PointOfSale,
                        heroIconContentDescription = null,
                    )
                }

                item {
                    MainSalesWorkflowCard(
                        windowSize = windowSize,
                        clients = clients,
                        sellable = sellable,
                        selectedClient = selectedClient,
                        selectedProduct = selectedProduct,
                        onClientSelected = { clientId = it.id },
                        onProductSelected = { productId = it.id },
                        qtyText = qtyText,
                        onQtyChange = { qtyText = it },
                        marginText = marginText,
                        onMarginChange = { marginText = it },
                        totalText = totalText,
                        onTotalChange = { totalText = it },
                        whenText = whenText,
                        onWhenChange = { whenText = it },
                        saleNotes = saleNotes,
                        onSaleNotesChange = { saleNotes = it },
                        costPreview = costPreview,
                        marginPercentLabel = marginText.trim(),
                        suggestLoading = suggestLoading,
                        onUseSuggestedClick = useSuggested@{
                            val p = selectedProduct ?: return@useSuggested
                            val q = qtyText.toDoubleOrNull() ?: return@useSuggested
                            val m = parseMarginPercent(marginText) ?: return@useSuggested
                            scope.launch {
                                suggestLoading = true
                                try {
                                    val preview =
                                        withContext(Dispatchers.IO) {
                                            repo.saleCostPreview(
                                                productId = p.id,
                                                quantitySold = q,
                                                marginPercent = m,
                                            )
                                        }
                                    if (preview != null) {
                                        totalText = formatMoney(preview.suggestedTotal)
                                    }
                                } finally {
                                    suggestLoading = false
                                }
                            }
                        },
                        useSuggestedEnabled =
                            selectedProduct != null &&
                                qtyText.toDoubleOrNull()?.let { it > 1e-12 } == true &&
                                parseMarginPercent(marginText) != null,
                        totalDisplayed = totalParsed?.let { formatMoney(it) } ?: "—",
                        error = error,
                        canSubmitSale = canSubmitSale && !submitting,
                        submitting = submitting,
                        onConfirmClick = confirm@{
                            error = null
                            if (!canSubmitSale) return@confirm
                            showSaleConfirmDialog = true
                        },
                        confirmGradient = confirmGradient,
                    )
                }

                item {
                    SectionContainer(
                        horizontalPadding = 0.dp,
                        verticalPadding = AppSpacing.sm,
                    ) {
                        Text(
                            text = "Ventas recientes",
                            style = AppTypography.SectionTitle,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Historial operativo con costos imputados y referencia de precio sugerido.",
                            style = AppTypography.BodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = AppSpacing.xs, bottom = AppSpacing.sm),
                        )
                    }
                }

                items(recent, key = { it.id }) { sale ->
                    SaleHistoryCard(sale = sale)
                }
            }

            AppFloatingActionButton(
                onClick = {
                    scope.launch {
                        listState.scrollToItem(0)
                    }
                },
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(AppSpacing.xl),
                gradient = fabGradient,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ir al inicio del formulario")
            }
        }
    }

    if (showSaleConfirmDialog) {
        val p = selectedProduct
        val c = selectedClient
        AlertDialog(
            onDismissRequest = { if (!submitting) showSaleConfirmDialog = false },
            title = { Text("¿Registrar esta venta?", style = AppTypography.SectionTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    Text(
                        "Revise los datos antes de confirmar.",
                        style = AppTypography.Body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text("Cliente: ${c?.name ?: "—"}", style = AppTypography.BodySmall)
                    if (p != null) {
                        Text(
                            "Lote: ${p.name} · ${p.stage.shortCode} · " +
                                "${if (p.isFailed) "Saldo" else "OK"} · disp. ${formatQty(p.quantity)}",
                            style = AppTypography.BodySmall,
                        )
                    }
                    Text(
                        "Cantidad: ${qtyParsed?.let { formatQty(it) } ?: "—"} postes",
                        style = AppTypography.BodySmall,
                    )
                    Text(
                        "Total cobrado: ${totalParsed?.let { formatMoney(it) } ?: "—"}",
                        style = AppTypography.BodySmall,
                        fontWeight = FontWeight.Medium,
                    )
                    Text("Fecha / hora: ${whenText.trim()}", style = AppTypography.BodySmall)
                    Text("% ganancia (estim.): ${marginText.trim()}", style = AppTypography.BodySmall)
                    if (saleNotes.isNotBlank()) {
                        Text("Notas: ${saleNotes.trim()}", style = AppTypography.BodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !submitting,
                    onClick = {
                        showSaleConfirmDialog = false
                        error = null
                        val cid = clientId ?: return@TextButton
                        val pid = productId ?: return@TextButton
                        val q = qtyParsed ?: return@TextButton
                        val total = totalParsed ?: return@TextButton
                        val whenMs = whenParsed ?: return@TextButton
                        val margin = marginParsed ?: return@TextButton
                        scope.launch {
                            submitting = true
                            try {
                                val result =
                                    withContext(Dispatchers.IO) {
                                        repo.recordSale(
                                            productId = pid,
                                            clientId = cid,
                                            quantitySold = q,
                                            totalAmount = total,
                                            soldAtEpochMs = whenMs,
                                            notes = saleNotes.trim().ifBlank { null },
                                            marginPercentForEstimate = margin,
                                        )
                                    }
                                when (result) {
                                    is InventoryRepository.SaleRecordingResult.Ok -> {
                                        clients =
                                            withContext(Dispatchers.IO) { repo.listClients() }
                                        sellable =
                                            withContext(Dispatchers.IO) { repo.listSellableProducts() }
                                        recent =
                                            withContext(Dispatchers.IO) { repo.listSales(80) }
                                        if (productId != null &&
                                            sellable.none { it.id == productId }
                                        ) {
                                            productId = sellable.firstOrNull()?.id
                                        }
                                        saleNotes = ""
                                        whenText =
                                            formatEpochMs(System.currentTimeMillis())
                                        marginText = "20"
                                        error = null
                                        formResetNonce++
                                    }
                                    is InventoryRepository.SaleRecordingResult.Err -> error = result.message
                                }
                            } finally {
                                submitting = false
                            }
                        }
                    },
                ) {
                    Text("Registrar venta")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !submitting,
                    onClick = { showSaleConfirmDialog = false },
                ) {
                    Text("Cancelar")
                }
            },
        )
    }
}

@Composable
private fun MainSalesWorkflowCard(
    windowSize: WindowSize,
    clients: List<Client>,
    sellable: List<Product>,
    selectedClient: Client?,
    selectedProduct: Product?,
    onClientSelected: (Client) -> Unit,
    onProductSelected: (Product) -> Unit,
    qtyText: String,
    onQtyChange: (String) -> Unit,
    marginText: String,
    onMarginChange: (String) -> Unit,
    totalText: String,
    onTotalChange: (String) -> Unit,
    whenText: String,
    onWhenChange: (String) -> Unit,
    saleNotes: String,
    onSaleNotesChange: (String) -> Unit,
    costPreview: SaleCostPreview?,
    marginPercentLabel: String,
    suggestLoading: Boolean,
    onUseSuggestedClick: () -> Unit,
    useSuggestedEnabled: Boolean,
    totalDisplayed: String,
    error: String?,
    canSubmitSale: Boolean,
    submitting: Boolean,
    onConfirmClick: () -> Unit,
    confirmGradient: Brush,
) {
    AppCard(
        shape = AppShapes.large,
        contentPadding = PaddingValues(AppSpacing.xl),
        enableHoverElevation = false,
        showHairlineBorder = true,
    ) {
        Column(
            modifier = Modifier.animateContentSize(animationSpec = tween(220)),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xl),
        ) {
            AppDropdownField(
                label = "Cliente",
                options = clients,
                selected = selectedClient,
                onSelected = onClientSelected,
                optionLabel = { it.name },
                placeholder = if (clients.isEmpty()) "Cree clientes primero" else "Elegir cliente…",
                enabled = clients.isNotEmpty(),
                searchable = true,
                searchPlaceholder = "Buscar cliente…",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.People,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )

            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                AppDropdownField(
                    label = "Lote a vender",
                    options = sellable,
                    selected = selectedProduct,
                    onSelected = onProductSelected,
                    optionLabel = { p ->
                        "${p.name} · ${p.stage.shortCode} · ${if (p.isFailed) "Saldo" else "OK"} · disp. ${formatQty(p.quantity)}"
                    },
                    placeholder = if (sellable.isEmpty()) "No hay lotes vendibles" else "Elegir lote…",
                    enabled = sellable.isNotEmpty(),
                    searchable = true,
                    searchPlaceholder = "Buscar lote…",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
                selectedProduct?.let { p ->
                    Text(
                        text =
                            "Precio referencia: ${p.effectiveSalePrice()?.let { formatMoney(it) } ?: "—"} / poste",
                        style = AppTypography.BodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (windowSize.widthClass == WindowWidthClass.Compact) {
                PricingSection(
                    qtyText = qtyText,
                    onQtyChange = onQtyChange,
                    marginText = marginText,
                    onMarginChange = onMarginChange,
                    totalText = totalText,
                    onTotalChange = onTotalChange,
                    suggestLoading = suggestLoading,
                    onUseSuggestedClick = onUseSuggestedClick,
                    useSuggestedEnabled = useSuggestedEnabled,
                )
                AnimatedVisibility(
                    visible = costPreview != null,
                    enter = fadeIn(tween(200)) + expandVertically(),
                    exit = fadeOut(tween(160)) + shrinkVertically(),
                ) {
                    costPreview?.let { prev ->
                        CostAnalysisPanel(
                            preview = prev,
                            marginPercentLabel = marginPercentLabel,
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1.15f),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
                    ) {
                        PricingSection(
                            qtyText = qtyText,
                            onQtyChange = onQtyChange,
                            marginText = marginText,
                            onMarginChange = onMarginChange,
                            totalText = totalText,
                            onTotalChange = onTotalChange,
                            suggestLoading = suggestLoading,
                            onUseSuggestedClick = onUseSuggestedClick,
                            useSuggestedEnabled = useSuggestedEnabled,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        AnimatedVisibility(
                            visible = costPreview != null,
                            enter = fadeIn(tween(200)) + expandVertically(),
                            exit = fadeOut(tween(160)) + shrinkVertically(),
                        ) {
                            costPreview?.let { prev ->
                                CostAnalysisPanel(
                                    preview = prev,
                                    marginPercentLabel = marginPercentLabel,
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                shape = AppShapes.medium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Total a cobrar",
                            style = AppTypography.Caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Monto final de la operación",
                            style = AppTypography.BodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = totalDisplayed,
                        style = AppTypography.MetricMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            MetadataSection(
                whenText = whenText,
                onWhenChange = onWhenChange,
                saleNotes = saleNotes,
                onSaleNotesChange = onSaleNotesChange,
            )

            error?.let {
                Text(
                    text = it,
                    style = AppTypography.BodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            ConfirmActionBar(
                canSubmitSale = canSubmitSale,
                submitting = submitting,
                onConfirmClick = onConfirmClick,
                confirmGradient = confirmGradient,
            )
        }
    }
}

@Composable
private fun PricingSection(
    qtyText: String,
    onQtyChange: (String) -> Unit,
    marginText: String,
    onMarginChange: (String) -> Unit,
    totalText: String,
    onTotalChange: (String) -> Unit,
    suggestLoading: Boolean,
    onUseSuggestedClick: () -> Unit,
    useSuggestedEnabled: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
        Text(
            text = "Precios y cantidad",
            style = AppTypography.CardTitle,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                AppNumberField(
                    value = qtyText,
                    onValueChange = onQtyChange,
                    label = "Cantidad",
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "postes",
                    style = AppTypography.BodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 20.dp),
                )
            }
            AppNumberField(
                value = marginText,
                onValueChange = onMarginChange,
                label = "% ganancia (estim.)",
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            AppTextField(
                value = totalText,
                onValueChange = onTotalChange,
                label = "Total cobrado",
                placeholder = "0,00",
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppOutlinedButton(
                    text = "Usar sugerido",
                    onClick = onUseSuggestedClick,
                    enabled = useSuggestedEnabled && !suggestLoading,
                    modifier = Modifier.fillMaxWidth(),
                    minHeight = 52.dp,
                    leadingIcon = {
                        if (suggestLoading) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun CostAnalysisPanel(
    preview: SaleCostPreview,
    marginPercentLabel: String,
) {
    AppCard(
        shape = AppShapes.medium,
        contentPadding = PaddingValues(AppSpacing.lg),
        enableHoverElevation = true,
        showHairlineBorder = true,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
            Text(
                text = "Estimación de costo y precio sugerido",
                style = AppTypography.CardTitle,
                color = MaterialTheme.colorScheme.onSurface,
            )
            CostRow(
                label = "Costo de adquisición (esta venta)",
                value = formatMoney(preview.acquisitionTotalForSaleQty),
                hint =
                    "Material ${formatMoney(preview.acquisitionMaterialTotalForSaleQty)} · " +
                        "Traslado ${formatMoney(preview.acquisitionTransportTotalForSaleQty)}",
            )
            CostRow(
                label = "Desglose / poste (sobre disponible del lote)",
                value =
                    "${formatMoney(preview.acquisitionMaterialPerPole ?: 0.0)} + " +
                        "${formatMoney(preview.acquisitionTransportPerPole)} + " +
                        formatMoney(preview.processingCostPerPole),
                hint = "→ ${formatMoney(preview.unitCostBasis)} costo total/poste",
                valueColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CostRow(
                label = "Procesamiento (esta cantidad)",
                value = formatMoney(preview.processingTotalForSaleQty),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            CostRow(
                label = "Precio sugerido / poste",
                value = formatMoney(preview.suggestedUnitPrice),
                hint = "Costo/poste × (1 + $marginPercentLabel %)",
                valueColor = AppThemeState.semantic.info,
            )
            Surface(
                shape = AppShapes.small,
                color = AppThemeState.semantic.success.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(AppSpacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Total sugerido",
                        style = AppTypography.CardTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = formatMoney(preview.suggestedTotal),
                        style = AppTypography.MetricMedium,
                        color = AppThemeState.semantic.success,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun CostRow(
    label: String,
    value: String,
    hint: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = AppTypography.Caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = AppTypography.Body,
            color = valueColor,
            fontWeight = FontWeight.Medium,
        )
        hint?.let {
            Text(
                text = it,
                style = AppTypography.BodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MetadataSection(
    whenText: String,
    onWhenChange: (String) -> Unit,
    saleNotes: String,
    onSaleNotesChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
        Text(
            text = "Detalles de la operación",
            style = AppTypography.CardTitle,
            color = MaterialTheme.colorScheme.onSurface,
        )
        AppTextField(
            value = whenText,
            onValueChange = onWhenChange,
            label = "Fecha / hora venta",
            placeholder = "yyyy-MM-dd HH:mm",
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
        AppTextArea(
            value = saleNotes,
            onValueChange = onSaleNotesChange,
            label = "Notas (opcional)",
            placeholder = "Condiciones, referencia interna, contacto…",
            minLines = 3,
            maxLines = 8,
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Notes,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
    }
}

@Composable
private fun ConfirmActionBar(
    canSubmitSale: Boolean,
    submitting: Boolean,
    onConfirmClick: () -> Unit,
    confirmGradient: Brush,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        AppButton(
            text = if (submitting) "Procesando…" else "Confirmar venta",
            onClick = onConfirmClick,
            enabled = canSubmitSale && !submitting,
            modifier = Modifier.fillMaxWidth(),
            gradient = confirmGradient,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            minHeight = 52.dp,
            leadingIcon = {
                if (submitting) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            },
        )
    }
}
