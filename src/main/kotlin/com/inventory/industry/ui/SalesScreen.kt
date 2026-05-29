package com.inventory.industry.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inventory.industry.data.Client
import com.inventory.industry.data.InventoryRepository
import com.inventory.industry.data.Product
import com.inventory.industry.data.SaleCostPreview
import com.inventory.industry.data.SaleRecord
import com.inventory.industry.ui.components.buttons.AppButton
import com.inventory.industry.ui.components.buttons.AppOutlinedButton
import com.inventory.industry.ui.components.cards.AppCard
import com.inventory.industry.ui.components.cards.SectionCard
import com.inventory.industry.ui.components.dashboard.FieldWithIcon
import com.inventory.industry.ui.components.dashboard.InlineBanner
import com.inventory.industry.ui.components.dashboard.KpiCard
import com.inventory.industry.ui.components.dashboard.SelectableCard
import com.inventory.industry.ui.components.dashboard.StatusDotBadge
import com.inventory.industry.ui.components.dashboard.SummaryLine
import com.inventory.industry.ui.components.dashboard.WizardStepper
import com.inventory.industry.ui.components.dashboard.monthDeltaColor
import com.inventory.industry.ui.components.dashboard.monthDeltaLabel
import com.inventory.industry.ui.components.feedback.EmptyState
import com.inventory.industry.ui.components.inputs.AppNumberField
import com.inventory.industry.ui.components.inputs.AppSearchField
import com.inventory.industry.ui.components.inputs.AppTextArea
import com.inventory.industry.ui.components.inputs.AppTextField
import com.inventory.industry.ui.components.sales.SaleLedgerStatus
import com.inventory.industry.ui.components.sales.toLedgerStatus
import com.inventory.industry.ui.navigation.ScreenRoute
import com.inventory.industry.ui.theme.AppShapes
import com.inventory.industry.ui.theme.AppSpacing
import com.inventory.industry.ui.theme.AppThemeState
import com.inventory.industry.ui.theme.AppTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

private val SALES_STEPS = listOf(
    "Datos de la venta",
    "Productos y cantidad",
    "Costos y precios",
    "Confirmar venta",
)

private fun bs(value: Double): String = "Bs ${formatMoney(value)}"

@OptIn(ExperimentalLayoutApi::class)
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

    var currentStep by remember { mutableStateOf(0) }
    var clientQuery by remember { mutableStateOf("") }
    var lotQuery by remember { mutableStateOf("") }
    var creatingClient by remember { mutableStateOf(false) }
    var detailSale by remember { mutableStateOf<SaleRecord?>(null) }

    val scope = rememberCoroutineScope()

    fun reloadLists() {
        scope.launch {
            clients = withContext(Dispatchers.IO) { repo.listClients() }
            sellable = withContext(Dispatchers.IO) { repo.listSellableProducts() }
            recent = withContext(Dispatchers.IO) { repo.listSales(80) }
        }
    }

    LaunchedEffect(Unit) { reloadLists() }
    LaunchedEffect(clients) { if (clientId == null && clients.isNotEmpty()) clientId = clients.first().id }
    LaunchedEffect(sellable) { if (productId == null && sellable.isNotEmpty()) productId = sellable.first().id }
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
        totalText = if (hint != null) formatMoney(hint * p.quantity) else ""
    }

    LaunchedEffect(selectedProduct?.id, qtyText, marginText) {
        val p = selectedProduct
        val q = qtyText.toDoubleOrNull()
        val m = parseMarginPercent(marginText)
        if (p == null || q == null || m == null || q <= 1e-12) {
            costPreview = null
            return@LaunchedEffect
        }
        costPreview = withContext(Dispatchers.IO) {
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
            clientId != null && clients.any { it.id == clientId } &&
            productId != null && sellable.any { it.id == productId } &&
            qtyParsed != null && qtyParsed > 1e-12 &&
            selectedProduct != null && qtyParsed <= selectedProduct.quantity + 1e-9 &&
            totalParsed != null && totalParsed >= 0 &&
            whenParsed != null && marginParsed != null

    // Derived figures for live summary / profit.
    val costForQty = costPreview?.let { it.acquisitionTotalForSaleQty + it.processingTotalForSaleQty }
    val unitPrice = if (qtyParsed != null && qtyParsed > 1e-12 && totalParsed != null) totalParsed / qtyParsed else null
    val profit = if (totalParsed != null && costForQty != null) totalParsed - costForQty else null
    val profitPct = if (profit != null && costForQty != null && costForQty > 1e-9) profit / costForQty * 100.0 else null

    val kpis = remember(recent, sellable) { computeSalesKpis(recent, sellable) }

    fun stepValid(step: Int): Boolean =
        when (step) {
            0 -> clientId != null && whenParsed != null
            1 ->
                productId != null && selectedProduct != null &&
                    qtyParsed != null && qtyParsed > 1e-12 &&
                    qtyParsed <= selectedProduct.quantity + 1e-9
            2 -> totalParsed != null && totalParsed >= 0 && marginParsed != null
            else -> canSubmitSale
        }

    fun stepError(step: Int): String =
        when (step) {
            0 -> "Seleccione un cliente y una fecha válida."
            1 -> "Seleccione un lote y una cantidad válida (no mayor a la disponible)."
            2 -> "Ingrese total y % de ganancia válidos."
            else -> "Revise los datos de la venta."
        }

    fun useSuggested() {
        val p = selectedProduct ?: return
        val q = qtyText.toDoubleOrNull() ?: return
        val m = parseMarginPercent(marginText) ?: return
        scope.launch {
            suggestLoading = true
            try {
                val preview = withContext(Dispatchers.IO) {
                    repo.saleCostPreview(productId = p.id, quantitySold = q, marginPercent = m)
                }
                if (preview != null) totalText = formatMoney(preview.suggestedTotal)
            } finally {
                suggestLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = AppSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Text("Ventas", style = AppTypography.PageTitle, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "Registre ventas de postes terminados (OK) o lotes fallados a precio de saldo. El precio sugerido " +
                    "aplica el % de ganancia sobre el costo por poste (material, traslado prorrateado y procesamiento).",
                style = AppTypography.BodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        error?.let { InlineBanner(it, isError = true) }

        // ---- 1. KPI dashboard header ------------------------------------------
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
            maxItemsInEachRow = 5,
        ) {
            val primary = MaterialTheme.colorScheme.primary
            val semantic = AppThemeState.semantic
            KpiCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.ShoppingCart,
                accent = primary,
                label = "Ventas (mes)",
                value = kpis.monthCount.toString(),
                subtitle = monthDeltaLabel(kpis.monthCount, kpis.prevMonthCount),
                subtitleColor = monthDeltaColor(kpis.monthCount, kpis.prevMonthCount, semantic.success, semantic.warning, MaterialTheme.colorScheme.onSurfaceVariant),
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.CheckCircle,
                accent = semantic.success,
                label = "Terminadas OK",
                value = kpis.okMonth.toString(),
                subtitle = monthDeltaLabel(kpis.okMonth, kpis.prevOkMonth),
                subtitleColor = monthDeltaColor(kpis.okMonth, kpis.prevOkMonth, semantic.success, semantic.warning, MaterialTheme.colorScheme.onSurfaceVariant),
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Inventory2,
                accent = semantic.info,
                label = "Por vender",
                value = kpis.sellableCount.toString(),
                subtitle = "Lotes disponibles",
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Payments,
                accent = MaterialTheme.colorScheme.tertiary,
                label = "Total facturado",
                value = bs(kpis.billedMonth),
                subtitle = "Este mes",
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Inventory,
                accent = semantic.warning,
                label = "Postes vendidos",
                value = formatQty(kpis.polesMonth),
                subtitle = "Este mes",
            )
        }

        // ---- 2 + 3 + 8 + 9. Wizard (left) + live summary (right) --------------
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val stacked = maxWidth < 980.dp
            val workflow: @Composable (Modifier) -> Unit = { mod ->
                SalesWorkflowCard(
                    modifier = mod,
                    currentStep = currentStep,
                    onStepClick = { currentStep = it },
                    onBack = { if (currentStep > 0) currentStep-- },
                    onNext = {
                        if (stepValid(currentStep)) {
                            error = null
                            if (currentStep < SALES_STEPS.lastIndex) currentStep++
                        } else {
                            error = stepError(currentStep)
                        }
                    },
                ) { step ->
                    when (step) {
                        0 ->
                            StepSaleData(
                                clients = clients,
                                query = clientQuery,
                                onQueryChange = { clientQuery = it },
                                selectedId = clientId,
                                onSelect = { clientId = it },
                                onAddClient = { creatingClient = true },
                                whenText = whenText,
                                onWhenChange = { whenText = it },
                                saleNotes = saleNotes,
                                onNotesChange = { saleNotes = it },
                            )
                        1 ->
                            StepProducts(
                                lots = sellable,
                                query = lotQuery,
                                onQueryChange = { lotQuery = it },
                                selectedId = productId,
                                onSelect = { productId = it },
                                qtyText = qtyText,
                                onQtyChange = { qtyText = it },
                                selectedProduct = selectedProduct,
                            )
                        2 ->
                            StepPricing(
                                marginText = marginText,
                                onMarginChange = { marginText = it },
                                totalText = totalText,
                                onTotalChange = { totalText = it },
                                unitPrice = unitPrice,
                                suggestLoading = suggestLoading,
                                useSuggestedEnabled = selectedProduct != null &&
                                    qtyText.toDoubleOrNull()?.let { it > 1e-12 } == true &&
                                    parseMarginPercent(marginText) != null,
                                onUseSuggested = { useSuggested() },
                                preview = costPreview,
                                marginLabel = marginText.trim(),
                            )
                        else ->
                            StepConfirmSale(
                                client = selectedClient,
                                product = selectedProduct,
                                whenText = whenText,
                                qty = qtyParsed,
                                total = totalParsed,
                                profit = profit,
                                profitPct = profitPct,
                            )
                    }
                }
            }
            val summary: @Composable (Modifier) -> Unit = { mod ->
                Column(modifier = mod, verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                    SalesSummaryCard(
                        client = selectedClient,
                        product = selectedProduct,
                        whenText = whenText,
                        qty = qtyParsed,
                        costForQty = costForQty,
                        marginPct = marginParsed,
                        unitPrice = unitPrice,
                        total = totalParsed,
                        profit = profit,
                        profitPct = profitPct,
                    )
                    SalesActionBar(
                        qty = qtyParsed,
                        total = totalParsed,
                        profit = profit,
                        enabled = canSubmitSale,
                        loading = submitting,
                        onConfirm = {
                            error = null
                            if (canSubmitSale) showSaleConfirmDialog = true
                        },
                    )
                }
            }

            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)) {
                    workflow(Modifier.fillMaxWidth())
                    summary(Modifier.fillMaxWidth())
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.lg),
                    verticalAlignment = Alignment.Top,
                ) {
                    workflow(Modifier.weight(1.7f))
                    summary(Modifier.weight(1f))
                }
            }
        }

        // ---- 10. Recent sales grid --------------------------------------------
        SectionCard(
            title = "Ventas recientes",
            subtitle = "Historial operativo con costos imputados y precio sugerido de referencia",
        ) {
            if (recent.isEmpty()) {
                EmptyState(
                    title = "Sin ventas registradas",
                    message = "Cuando registre una venta aparecerá aquí.",
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                    maxItemsInEachRow = 3,
                ) {
                    recent.forEach { sale ->
                        RecentSaleCard(
                            modifier = Modifier.weight(1f),
                            sale = sale,
                            onDetail = { detailSale = sale },
                        )
                    }
                }
            }
        }
    }

    if (creatingClient) {
        ClientEditorDialog(
            onDismiss = { creatingClient = false },
            onSave = { name, contact, notes ->
                scope.launch {
                    val newId = withContext(Dispatchers.IO) { repo.upsertClient(null, name, contact, notes) }
                    creatingClient = false
                    clients = withContext(Dispatchers.IO) { repo.listClients() }
                    clientId = newId
                }
            },
        )
    }

    detailSale?.let { sale ->
        SaleDetailDialog(sale = sale, onDismiss = { detailSale = null })
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
                    Text("Cantidad: ${qtyParsed?.let { formatQty(it) } ?: "—"} postes", style = AppTypography.BodySmall)
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
                                val result = withContext(Dispatchers.IO) {
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
                                        clients = withContext(Dispatchers.IO) { repo.listClients() }
                                        sellable = withContext(Dispatchers.IO) { repo.listSellableProducts() }
                                        recent = withContext(Dispatchers.IO) { repo.listSales(80) }
                                        if (productId != null && sellable.none { it.id == productId }) {
                                            productId = sellable.firstOrNull()?.id
                                        }
                                        saleNotes = ""
                                        whenText = formatEpochMs(System.currentTimeMillis())
                                        marginText = "20"
                                        error = null
                                        currentStep = 0
                                        formResetNonce++
                                    }
                                    is InventoryRepository.SaleRecordingResult.Err -> error = result.message
                                }
                            } finally {
                                submitting = false
                            }
                        }
                    },
                ) { Text("Registrar venta") }
            },
            dismissButton = {
                TextButton(enabled = !submitting, onClick = { showSaleConfirmDialog = false }) { Text("Cancelar") }
            },
        )
    }
}

// =====================================================================================
// KPIs
// =====================================================================================

private data class SalesKpis(
    val monthCount: Int,
    val prevMonthCount: Int,
    val okMonth: Int,
    val prevOkMonth: Int,
    val sellableCount: Int,
    val billedMonth: Double,
    val polesMonth: Double,
)

private fun computeSalesKpis(recent: List<SaleRecord>, sellable: List<Product>): SalesKpis {
    val zone = ZoneId.systemDefault()
    val firstOfMonth = LocalDate.now().withDayOfMonth(1)
    val startThisMonth = firstOfMonth.atStartOfDay(zone).toInstant().toEpochMilli()
    val startNextMonth = firstOfMonth.plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val startPrevMonth = firstOfMonth.minusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()

    val thisMonth = recent.filter { it.soldAtEpochMs in startThisMonth until startNextMonth }
    val prevMonth = recent.filter { it.soldAtEpochMs in startPrevMonth until startThisMonth }

    fun isOk(s: SaleRecord) = s.toLedgerStatus() == SaleLedgerStatus.CompletedOk

    return SalesKpis(
        monthCount = thisMonth.size,
        prevMonthCount = prevMonth.size,
        okMonth = thisMonth.count(::isOk),
        prevOkMonth = prevMonth.count(::isOk),
        sellableCount = sellable.size,
        billedMonth = thisMonth.sumOf { it.totalAmount },
        polesMonth = thisMonth.sumOf { it.quantitySold },
    )
}

// =====================================================================================
// Workflow shell
// =====================================================================================

@Composable
private fun SalesWorkflowCard(
    currentStep: Int,
    onStepClick: (Int) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit,
) {
    SectionCard(
        modifier = modifier,
        title = "Registrar venta",
        subtitle = SALES_STEPS[currentStep],
    ) {
        WizardStepper(steps = SALES_STEPS, currentStep = currentStep, onStepClick = onStepClick)
        LinearProgressIndicator(
            progress = { (currentStep + 1) / SALES_STEPS.size.toFloat() },
            modifier = Modifier.fillMaxWidth().clip(AppShapes.small),
        )
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState >= initialState) {
                    (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 6 }) togetherWith
                        (fadeOut(tween(150)) + slideOutHorizontally(tween(150)) { -it / 6 })
                } else {
                    (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { -it / 6 }) togetherWith
                        (fadeOut(tween(150)) + slideOutHorizontally(tween(150)) { it / 6 })
                }
            },
            label = "salesStep",
        ) { step ->
            Box(modifier = Modifier.fillMaxWidth()) { content(step) }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            AppOutlinedButton(
                text = "Atrás",
                onClick = onBack,
                enabled = currentStep > 0,
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
            Spacer(modifier = Modifier.weight(1f))
            if (currentStep < SALES_STEPS.lastIndex) {
                AppButton(
                    text = "Siguiente",
                    onClick = onNext,
                    trailingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
            }
        }
    }
}

// =====================================================================================
// Step 1 — Sale data (client + date + notes)
// =====================================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepSaleData(
    clients: List<Client>,
    query: String,
    onQueryChange: (String) -> Unit,
    selectedId: Int?,
    onSelect: (Int) -> Unit,
    onAddClient: () -> Unit,
    whenText: String,
    onWhenChange: (String) -> Unit,
    saleNotes: String,
    onNotesChange: (String) -> Unit,
) {
    val filtered = clients.filter {
        query.isBlank() ||
            it.name.contains(query, ignoreCase = true) ||
            (it.contact?.contains(query, ignoreCase = true) == true)
    }
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
        Text("Cliente", style = AppTypography.CardTitle, color = MaterialTheme.colorScheme.onSurface)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppSearchField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = "Buscar cliente por nombre o contacto…",
                keyboardShortcutHint = null,
            )
            AppButton(
                text = "Nuevo cliente",
                onClick = onAddClient,
                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
        }
        if (clients.isEmpty()) {
            EmptyState(
                title = "Sin clientes",
                message = "Agregue un cliente para registrar ventas.",
            )
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                maxItemsInEachRow = 3,
            ) {
                filtered.forEach { client ->
                    ClientCard(
                        modifier = Modifier.weight(1f),
                        client = client,
                        selected = client.id == selectedId,
                        onSelect = { onSelect(client.id) },
                    )
                }
            }
        }
        HorizontalDivider(color = AppThemeState.semantic.border.copy(alpha = 0.4f))
        FieldWithIcon(Icons.Filled.CalendarMonth) {
            AppTextField(
                value = whenText,
                onValueChange = onWhenChange,
                modifier = Modifier.weight(1f),
                label = "Fecha / hora venta (yyyy-MM-dd HH:mm)",
            )
        }
        FieldWithIcon(Icons.AutoMirrored.Filled.Notes, alignTop = true) {
            AppTextArea(
                value = saleNotes,
                onValueChange = onNotesChange,
                modifier = Modifier.weight(1f),
                label = "Notas (opcional)",
                minLines = 3,
                maxLines = 6,
            )
        }
    }
}

@Composable
private fun ClientCard(
    client: Client,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectableCard(selected = selected, onClick = onSelect, modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            val accent = MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(accent.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    client.name.firstOrNull()?.uppercase() ?: "?",
                    style = AppTypography.CardTitle,
                    color = accent,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    client.name,
                    style = AppTypography.CardTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "Cliente #${client.id}" + (client.contact?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                    style = AppTypography.BodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (selected) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Seleccionado",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

// =====================================================================================
// Step 2 — Product / lot selection + quantity
// =====================================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepProducts(
    lots: List<Product>,
    query: String,
    onQueryChange: (String) -> Unit,
    selectedId: Int?,
    onSelect: (Int) -> Unit,
    qtyText: String,
    onQtyChange: (String) -> Unit,
    selectedProduct: Product?,
) {
    val filtered = lots.filter {
        query.isBlank() ||
            it.name.contains(query, ignoreCase = true) ||
            it.productLine.contains(query, ignoreCase = true)
    }
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
        AppSearchField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Buscar lote por nombre o línea…",
            keyboardShortcutHint = null,
        )
        if (lots.isEmpty()) {
            EmptyState(
                title = "No hay lotes vendibles",
                message = "Solo se pueden vender postes en Terminado (OK) o lotes fallados (saldo).",
            )
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                maxItemsInEachRow = 3,
            ) {
                filtered.forEach { lot ->
                    LotSaleCard(
                        modifier = Modifier.weight(1f),
                        lot = lot,
                        selected = lot.id == selectedId,
                        onSelect = { onSelect(lot.id) },
                    )
                }
            }
        }
        HorizontalDivider(color = AppThemeState.semantic.border.copy(alpha = 0.4f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            AppNumberField(
                value = qtyText,
                onValueChange = onQtyChange,
                label = "Cantidad (postes)",
                modifier = Modifier.weight(1f),
            )
            selectedProduct?.let { p ->
                Text(
                    "Disponible: ${formatQty(p.quantity)}",
                    style = AppTypography.BodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(top = AppSpacing.md),
                )
            }
        }
    }
}

@Composable
private fun LotSaleCard(
    lot: Product,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectableCard(selected = selected, onClick = onSelect, modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                lot.name,
                style = AppTypography.CardTitle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Seleccionado",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        StatusDotBadge(
            text = if (lot.isFailed) "Saldo · ${lot.stage.shortCode}" else "Terminado OK",
            kind = if (lot.isFailed) com.inventory.industry.ui.models.StatusKind.Warning else com.inventory.industry.ui.models.StatusKind.Success,
        )
        SummaryLine("Disponible", "${formatQty(lot.quantity)} u.")
        SummaryLine(
            "Precio ref.",
            lot.effectiveSalePrice()?.let { bs(it) } ?: "—",
            valueColor = AppThemeState.semantic.info,
        )
    }
}

// =====================================================================================
// Step 3 — Pricing workspace + estimation
// =====================================================================================

@Composable
private fun StepPricing(
    marginText: String,
    onMarginChange: (String) -> Unit,
    totalText: String,
    onTotalChange: (String) -> Unit,
    unitPrice: Double?,
    suggestLoading: Boolean,
    useSuggestedEnabled: Boolean,
    onUseSuggested: () -> Unit,
    preview: SaleCostPreview?,
    marginLabel: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            FieldWithIcon(Icons.Filled.Percent, modifier = Modifier.weight(1f)) {
                AppNumberField(
                    value = marginText,
                    onValueChange = onMarginChange,
                    modifier = Modifier.weight(1f),
                    label = "% ganancia (estim.)",
                )
            }
            FieldWithIcon(Icons.Filled.Payments, modifier = Modifier.weight(1f)) {
                AppTextField(
                    value = totalText,
                    onValueChange = onTotalChange,
                    modifier = Modifier.weight(1f),
                    label = "Total a cobrar (Bs)",
                    placeholder = "0,00",
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppOutlinedButton(
                text = "Usar precio sugerido",
                onClick = onUseSuggested,
                enabled = useSuggestedEnabled && !suggestLoading,
                modifier = Modifier.weight(1f),
                leadingIcon = {
                    if (suggestLoading) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                },
            )
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("Precio por unidad", style = AppTypography.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    unitPrice?.let { bs(it) } ?: "—",
                    style = AppTypography.CardTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        EstimationCard(preview = preview, marginLabel = marginLabel)
    }
}

@Composable
private fun EstimationCard(
    preview: SaleCostPreview?,
    marginLabel: String,
) {
    AppCard(modifier = Modifier.fillMaxWidth(), showHairlineBorder = true) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            Text(
                "Estimación de costo y precio sugerido",
                style = AppTypography.CardTitle,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (preview == null) {
                Text(
                    "Seleccione lote, cantidad y % de ganancia para ver la estimación.",
                    style = AppTypography.BodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    "Costo de adquisición",
                    style = AppTypography.BodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                SummaryLine("Material", bs(preview.acquisitionMaterialTotalForSaleQty))
                SummaryLine("Procesamiento", bs(preview.processingTotalForSaleQty))
                SummaryLine("Traslado", bs(preview.acquisitionTransportTotalForSaleQty))
                HorizontalDivider(color = AppThemeState.semantic.border.copy(alpha = 0.4f))
                SummaryLine("Costo por unidad", bs(preview.unitCostBasis))
                SummaryLine(
                    "Precio sugerido / poste",
                    bs(preview.suggestedUnitPrice),
                    valueColor = AppThemeState.semantic.info,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShapes.small)
                        .background(AppThemeState.semantic.success.copy(alpha = 0.12f))
                        .padding(AppSpacing.md),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Total sugerido", style = AppTypography.CardTitle, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                bs(preview.suggestedTotal),
                                style = AppTypography.MetricMedium,
                                color = AppThemeState.semantic.success,
                            )
                        }
                        val suggestedProfit = preview.suggestedTotal - (preview.acquisitionTotalForSaleQty + preview.processingTotalForSaleQty)
                        Text(
                            "Margen estimado: ${bs(suggestedProfit)} ($marginLabel %)",
                            style = AppTypography.BodySmall,
                            color = AppThemeState.semantic.success,
                        )
                    }
                }
            }
        }
    }
}

// =====================================================================================
// Step 4 — Confirm
// =====================================================================================

@Composable
private fun StepConfirmSale(
    client: Client?,
    product: Product?,
    whenText: String,
    qty: Double?,
    total: Double?,
    profit: Double?,
    profitPct: Double?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Text(
            "Revise la venta antes de confirmarla.",
            style = AppTypography.BodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SummaryLine("Cliente", client?.name ?: "—")
        SummaryLine("Producto", product?.name ?: "—")
        SummaryLine("Fecha", whenText.ifBlank { "—" })
        SummaryLine("Cantidad", qty?.let { "${formatQty(it)} postes" } ?: "—")
        SummaryLine("Precio total", total?.let { bs(it) } ?: "—")
        HorizontalDivider(color = AppThemeState.semantic.border.copy(alpha = 0.4f))
        SummaryLine(
            "Ganancia estimada",
            profit?.let { bs(it) + (profitPct?.let { p -> " (${formatMoney(p)} %)" } ?: "") } ?: "—",
            valueColor = AppThemeState.semantic.success,
        )
    }
}

// =====================================================================================
// Right column — live summary + action bar
// =====================================================================================

@Composable
private fun SalesSummaryCard(
    client: Client?,
    product: Product?,
    whenText: String,
    qty: Double?,
    costForQty: Double?,
    marginPct: Double?,
    unitPrice: Double?,
    total: Double?,
    profit: Double?,
    profitPct: Double?,
) {
    SectionCard(title = "Resumen de la venta", subtitle = "Cálculo en tiempo real") {
        SummaryLine("Cliente", client?.name ?: "Sin seleccionar")
        SummaryLine("Producto", product?.name ?: "—")
        SummaryLine("Fecha", whenText.ifBlank { "—" })
        SummaryLine("Cantidad", qty?.let { "${formatQty(it)} postes" } ?: "—")
        SummaryLine("Costo total", costForQty?.let { bs(it) } ?: "—")
        SummaryLine("Ganancia %", marginPct?.let { "${formatMoney(it)} %" } ?: "—")
        SummaryLine("Precio unitario", unitPrice?.let { bs(it) } ?: "—")
        HorizontalDivider(color = AppThemeState.semantic.border.copy(alpha = 0.4f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Precio total", style = AppTypography.Body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                total?.let { bs(it) } ?: "—",
                style = AppTypography.MetricMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.small)
                .background(AppThemeState.semantic.success.copy(alpha = 0.12f))
                .padding(AppSpacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Margen estimado", style = AppTypography.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        profitPct?.let { "${formatMoney(it)} %" } ?: "—",
                        style = AppTypography.BodySmall,
                        color = AppThemeState.semantic.success,
                    )
                }
                Text(
                    profit?.let { bs(it) } ?: "—",
                    style = AppTypography.MetricMedium,
                    color = AppThemeState.semantic.success,
                )
            }
        }
    }
}

@Composable
private fun SalesActionBar(
    qty: Double?,
    total: Double?,
    profit: Double?,
    enabled: Boolean,
    loading: Boolean,
    onConfirm: () -> Unit,
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            SummaryLine("Cantidad", qty?.let { "${formatQty(it)} postes" } ?: "—")
            SummaryLine("Precio total", total?.let { bs(it) } ?: "—")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Ganancia estimada", style = AppTypography.CardTitle, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    profit?.let { bs(it) } ?: "—",
                    style = AppTypography.CardTitle.copy(fontWeight = FontWeight.SemiBold),
                    color = AppThemeState.semantic.success,
                )
            }
            AppButton(
                text = if (loading) "Procesando…" else "Confirmar venta",
                onClick = onConfirm,
                enabled = enabled && !loading,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                },
            )
        }
    }
}

// =====================================================================================
// Recent sales — grid cards + detail dialog
// =====================================================================================

@Composable
private fun RecentSaleCard(
    sale: SaleRecord,
    onDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val status = sale.toLedgerStatus()
    AppCard(modifier = modifier, showHairlineBorder = true) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        sale.clientName,
                        style = AppTypography.CardTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        formatEpochMs(sale.soldAtEpochMs),
                        style = AppTypography.Caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusDotBadge(text = status.label, kind = status.kind)
            }
            Text(
                "${sale.snapshotProductName} · ${sale.snapshotStage.shortCode}",
                style = AppTypography.BodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            HorizontalDivider(color = AppThemeState.semantic.border.copy(alpha = 0.4f))
            SummaryLine("Cantidad", "${formatQty(sale.quantitySold)} postes")
            SummaryLine("Costo imputado", bs(sale.snapshotAcquisitionCostTotal + sale.snapshotProcessingCostTotal))
            SummaryLine("Total cobrado", bs(sale.totalAmount))
            sale.snapshotSuggestedTotal?.let {
                SummaryLine("Sugerido", bs(it), valueColor = AppThemeState.semantic.info)
            }
            AppOutlinedButton(
                text = "Ver detalle",
                onClick = onDetail,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SaleDetailDialog(
    sale: SaleRecord,
    onDismiss: () -> Unit,
) {
    val status = sale.toLedgerStatus()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Venta · ${sale.snapshotProductName}", style = AppTypography.SectionTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                StatusDotBadge(text = status.label, kind = status.kind)
                SummaryLine("Cliente", sale.clientName)
                SummaryLine("Producto", "${sale.snapshotProductName} · ${sale.snapshotProductLine}")
                SummaryLine("Etapa", sale.snapshotStage.shortCode)
                SummaryLine("Fecha", formatEpochMs(sale.soldAtEpochMs))
                SummaryLine("Cantidad", "${formatQty(sale.quantitySold)} postes")
                HorizontalDivider(color = AppThemeState.semantic.border.copy(alpha = 0.4f))
                SummaryLine("Material", bs(sale.snapshotAcquisitionMaterialTotal))
                SummaryLine("Traslado", bs(sale.snapshotAcquisitionTransportTotal))
                SummaryLine("Procesamiento", bs(sale.snapshotProcessingCostTotal))
                SummaryLine("Costo adquisición", bs(sale.snapshotAcquisitionCostTotal))
                HorizontalDivider(color = AppThemeState.semantic.border.copy(alpha = 0.4f))
                SummaryLine("Total cobrado", bs(sale.totalAmount))
                sale.snapshotSuggestedTotal?.let { SummaryLine("Total sugerido", bs(it), valueColor = AppThemeState.semantic.info) }
                sale.snapshotMarginPercent?.let { SummaryLine("% ganancia", "${formatMoney(it)} %") }
                sale.notes?.takeIf { it.isNotBlank() }?.let {
                    Text("Notas: $it", style = AppTypography.BodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
    )
}

// =====================================================================================
// Quick add client dialog
// =====================================================================================

@Composable
private fun ClientEditorDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, contact: String?, notes: String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo cliente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                AppTextField(value = name, onValueChange = { name = it }, label = "Nombre", modifier = Modifier.fillMaxWidth())
                AppTextField(value = contact, onValueChange = { contact = it }, label = "Contacto (opcional)", modifier = Modifier.fillMaxWidth())
                AppTextField(value = notes, onValueChange = { notes = it }, label = "Notas (opcional)", modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onSave(name.trim(), contact.trim().ifBlank { null }, notes.trim().ifBlank { null }) },
                enabled = name.isNotBlank(),
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
