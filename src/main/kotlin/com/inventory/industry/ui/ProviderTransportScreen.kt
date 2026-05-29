package com.inventory.industry.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inventory.industry.data.Driver
import com.inventory.industry.data.InventoryRepository
import com.inventory.industry.data.Product
import com.inventory.industry.data.ProviderTransportRun
import com.inventory.industry.data.ProviderTransportRunStatus
import com.inventory.industry.ui.components.buttons.AppButton
import com.inventory.industry.ui.components.buttons.AppOutlinedButton
import com.inventory.industry.ui.components.cards.AppCard
import com.inventory.industry.ui.components.cards.SectionCard
import com.inventory.industry.ui.components.dashboard.FieldWithIcon
import com.inventory.industry.ui.components.dashboard.InlineBanner
import com.inventory.industry.ui.components.dashboard.KpiCard
import com.inventory.industry.ui.components.dashboard.SelectableCard
import com.inventory.industry.ui.components.dashboard.monthDeltaColor
import com.inventory.industry.ui.components.dashboard.monthDeltaLabel
import com.inventory.industry.ui.components.dashboard.StatusDotBadge
import com.inventory.industry.ui.components.dashboard.SummaryLine
import com.inventory.industry.ui.components.dashboard.WizardStepper
import com.inventory.industry.ui.components.dashboard.statusKindColor
import com.inventory.industry.ui.components.feedback.EmptyState
import com.inventory.industry.ui.components.feedback.StatusChip
import com.inventory.industry.ui.components.inputs.AppNumberField
import com.inventory.industry.ui.components.inputs.AppSearchField
import com.inventory.industry.ui.components.inputs.AppTextArea
import com.inventory.industry.ui.components.inputs.AppTextField
import com.inventory.industry.ui.models.StatusKind
import com.inventory.industry.ui.theme.AppElevations
import com.inventory.industry.ui.theme.AppShapes
import com.inventory.industry.ui.theme.AppSpacing
import com.inventory.industry.ui.theme.AppThemeState
import com.inventory.industry.ui.theme.AppTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val WIZARD_STEPS = listOf(
    "Seleccionar Chofer",
    "Datos del Transporte",
    "Seleccionar Lotes",
    "Confirmar Traslado",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProviderTransportScreen(repo: InventoryRepository) {
    var drivers by remember { mutableStateOf<List<Driver>>(emptyList()) }
    var eligibleLots by remember { mutableStateOf<List<Product>>(emptyList()) }
    var runs by remember { mutableStateOf<List<ProviderTransportRun>>(emptyList()) }
    var driverEditor by remember { mutableStateOf<Driver?>(null) }
    var creatingDriver by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var completeRun by remember { mutableStateOf<ProviderTransportRun?>(null) }
    var deleteDriverError by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }

    var currentStep by remember { mutableStateOf(0) }
    var driverQuery by remember { mutableStateOf("") }
    var selectedDriverId by remember { mutableStateOf<Int?>(null) }
    var vehiclePlate by remember { mutableStateOf("") }
    var freightText by remember { mutableStateOf("0") }
    var gruaText by remember { mutableStateOf("0") }
    var departedText by remember { mutableStateOf(formatEpochMs(System.currentTimeMillis())) }
    var expectedArrivalText by remember { mutableStateOf("") }
    var transportNotes by remember { mutableStateOf("") }
    var selectedLotIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            drivers = withContext(Dispatchers.IO) { repo.listDrivers() }
            eligibleLots = withContext(Dispatchers.IO) { repo.listProductsReadyPickupAtProvider() }
            runs = withContext(Dispatchers.IO) { repo.listProviderTransportRuns(80) }
        }
    }

    LaunchedEffect(Unit) { reload() }

    LaunchedEffect(drivers) {
        if (selectedDriverId == null && drivers.isNotEmpty()) {
            selectedDriverId = drivers.first().id
        }
    }

    val selectedDriver = drivers.firstOrNull { it.id == selectedDriverId }
    val freight = parseMoneyAmount(freightText) ?: 0.0
    val grua = parseMoneyAmount(gruaText) ?: 0.0
    val totalCost = freight + grua
    val selectedLots = remember(eligibleLots, selectedLotIds) {
        eligibleLots.filter { it.id in selectedLotIds }
    }
    val kpis = remember(runs) { computeTransportKpis(runs) }

    val canSubmit =
        selectedDriverId != null &&
            vehiclePlate.isNotBlank() &&
            parseDateTime(departedText) != null &&
            parseMoneyAmount(freightText) != null &&
            parseMoneyAmount(gruaText) != null &&
            (expectedArrivalText.isBlank() || parseDateTime(expectedArrivalText) != null) &&
            selectedLotIds.isNotEmpty()

    fun stepValid(step: Int): Boolean =
        when (step) {
            0 -> selectedDriverId != null
            1 ->
                vehiclePlate.isNotBlank() &&
                    parseDateTime(departedText) != null &&
                    parseMoneyAmount(freightText) != null &&
                    parseMoneyAmount(gruaText) != null &&
                    (expectedArrivalText.isBlank() || parseDateTime(expectedArrivalText) != null)
            2 -> selectedLotIds.isNotEmpty()
            else -> canSubmit
        }

    fun stepError(step: Int): String =
        when (step) {
            0 -> "Seleccione un chofer para continuar."
            1 -> "Complete patente, fechas y costos válidos."
            2 -> "Seleccione al menos un lote en predio proveedor."
            else -> "Revise los datos del traslado."
        }

    fun resetWizard() {
        selectedLotIds = emptySet()
        vehiclePlate = ""
        freightText = "0"
        gruaText = "0"
        expectedArrivalText = ""
        transportNotes = ""
        currentStep = 0
    }

    fun startTransfer() {
        errorMsg = null
        val did = selectedDriverId ?: run { errorMsg = stepError(0); currentStep = 0; return }
        val dep = parseDateTime(departedText) ?: run { errorMsg = "Fecha de salida inválida."; currentStep = 1; return }
        val freightV = parseMoneyAmount(freightText) ?: run { errorMsg = "Costo de transporte inválido."; currentStep = 1; return }
        val gruaV = parseMoneyAmount(gruaText) ?: run { errorMsg = "Costo de grúa inválido."; currentStep = 1; return }
        if (vehiclePlate.isBlank()) { errorMsg = "Indique patente o identificación del vehículo."; currentStep = 1; return }
        val exp = expectedArrivalText.trim().takeIf { it.isNotEmpty() }?.let { parseDateTime(it) }
        if (expectedArrivalText.isNotBlank() && exp == null) { errorMsg = "Fecha de llegada estimada inválida."; currentStep = 1; return }
        if (selectedLotIds.isEmpty()) { errorMsg = "Seleccione al menos un lote."; currentStep = 2; return }
        submitting = true
        scope.launch {
            val r =
                withContext(Dispatchers.IO) {
                    repo.startProviderTransport(
                        driverId = did,
                        vehiclePlate = vehiclePlate,
                        freightCost = freightV,
                        gruaCost = gruaV,
                        departedAtEpochMs = dep,
                        expectedArrivalEpochMs = exp,
                        productIds = selectedLotIds.toList(),
                        notes = transportNotes.trim().ifBlank { null },
                    )
                }
            submitting = false
            when (r) {
                is InventoryRepository.TransportRunResult.Ok -> {
                    resetWizard()
                    reload()
                }
                is InventoryRepository.TransportRunResult.Err -> errorMsg = r.message
            }
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = AppSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Text(
                "Traslados",
                style = AppTypography.PageTitle,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Gestión de transporte desde el predio del proveedor a planta. Cree el envío paso a paso; " +
                    "al registrar la llegada, flete y grúa se prorratean entre los lotes según cantidad de postes.",
                style = AppTypography.BodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        errorMsg?.let { msg -> InlineBanner(msg, isError = true) }
        deleteDriverError?.let { msg -> InlineBanner(msg, isError = true) }

        // ---- 1. KPI summary row -------------------------------------------------
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
                icon = Icons.Filled.LocalShipping,
                accent = primary,
                label = "Traslados (mes)",
                value = kpis.monthCount.toString(),
                subtitle = monthDeltaLabel(kpis.monthCount, kpis.prevMonthCount),
                subtitleColor = monthDeltaColor(kpis.monthCount, kpis.prevMonthCount, semantic.success, semantic.warning, MaterialTheme.colorScheme.onSurfaceVariant),
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.DirectionsCar,
                accent = semantic.warning,
                label = "En tránsito",
                value = kpis.inTransit.toString(),
                subtitle = "Envíos activos",
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.CheckCircle,
                accent = semantic.success,
                label = "Completados",
                value = kpis.completedMonth.toString(),
                subtitle = "Este mes",
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Payments,
                accent = semantic.info,
                label = "Costo total",
                value = "Bs ${formatMoney(kpis.costMonth)}",
                subtitle = "Transporte + grúa (mes)",
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Inventory2,
                accent = MaterialTheme.colorScheme.tertiary,
                label = "Lotes trasladados",
                value = kpis.lotsMonth.toString(),
                subtitle = "Este mes",
            )
        }

        // ---- 2 + 3 + 7 + 10. Wizard (left) + live summary (right) --------------
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val stacked = maxWidth < 980.dp
            val workflow: @Composable (Modifier) -> Unit = { mod ->
                WorkflowCard(
                    modifier = mod,
                    currentStep = currentStep,
                    onStepClick = { currentStep = it },
                    onBack = { if (currentStep > 0) currentStep-- },
                    onNext = {
                        if (stepValid(currentStep)) {
                            errorMsg = null
                            if (currentStep < WIZARD_STEPS.lastIndex) currentStep++
                        } else {
                            errorMsg = stepError(currentStep)
                        }
                    },
                ) {
                    when (currentStep) {
                        0 ->
                            StepDrivers(
                                drivers = drivers,
                                query = driverQuery,
                                onQueryChange = { driverQuery = it },
                                selectedId = selectedDriverId,
                                onSelect = { selectedDriverId = it },
                                onAdd = { creatingDriver = true; driverEditor = null },
                                onEdit = { driverEditor = it; creatingDriver = false },
                                onDelete = { d ->
                                    scope.launch {
                                        deleteDriverError = null
                                        val ok = withContext(Dispatchers.IO) { repo.deleteDriver(d.id) }
                                        if (ok) {
                                            if (selectedDriverId == d.id) {
                                                selectedDriverId = drivers.firstOrNull { it.id != d.id }?.id
                                            }
                                            reload()
                                        } else {
                                            deleteDriverError =
                                                "No se puede borrar: el chofer tiene traslados registrados."
                                        }
                                    }
                                },
                            )
                        1 ->
                            StepTransport(
                                vehiclePlate = vehiclePlate,
                                onPlate = { vehiclePlate = it },
                                freightText = freightText,
                                onFreight = { freightText = it },
                                gruaText = gruaText,
                                onGrua = { gruaText = it },
                                departedText = departedText,
                                onDeparted = { departedText = it },
                                expectedArrivalText = expectedArrivalText,
                                onExpectedArrival = { expectedArrivalText = it },
                                transportNotes = transportNotes,
                                onNotes = { transportNotes = it },
                            )
                        2 ->
                            StepLots(
                                lots = eligibleLots,
                                selectedIds = selectedLotIds,
                                onToggle = { id, on ->
                                    selectedLotIds = if (on) selectedLotIds + id else selectedLotIds - id
                                },
                            )
                        else ->
                            StepConfirm(
                                driver = selectedDriver,
                                vehiclePlate = vehiclePlate,
                                freight = freight,
                                grua = grua,
                                departedText = departedText,
                                expectedArrivalText = expectedArrivalText,
                                lots = selectedLots,
                            )
                    }
                }
            }
            val summary: @Composable (Modifier) -> Unit = { mod ->
                Column(modifier = mod, verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                    TransferSummaryCard(
                        driver = selectedDriver,
                        vehiclePlate = vehiclePlate,
                        freight = freight,
                        grua = grua,
                        total = totalCost,
                        lots = selectedLots,
                    )
                    PrimaryActionBar(
                        lotCount = selectedLots.size,
                        freight = freight,
                        grua = grua,
                        total = totalCost,
                        enabled = canSubmit,
                        loading = submitting,
                        onStart = { startTransfer() },
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
                    workflow(Modifier.weight(1.5f))
                    summary(Modifier.weight(1f))
                }
            }
        }

        // ---- 8. Shipment history timeline --------------------------------------
        SectionCard(
            title = "Historial de envíos",
            subtitle = "Línea de tiempo de traslados desde proveedor",
        ) {
            if (runs.isEmpty()) {
                EmptyState(
                    title = "Sin envíos registrados",
                    message = "Cuando inicie un traslado aparecerá aquí su seguimiento.",
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    runs.forEachIndexed { index, run ->
                        TimelineTransferCard(
                            run = run,
                            isLast = index == runs.lastIndex,
                            onComplete = { completeRun = run },
                            onCancel = {
                                scope.launch {
                                    val r = withContext(Dispatchers.IO) { repo.cancelProviderTransport(run.id) }
                                    when (r) {
                                        is InventoryRepository.TransportRunResult.Ok -> reload()
                                        is InventoryRepository.TransportRunResult.Err -> errorMsg = r.message
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    if (creatingDriver || driverEditor != null) {
        DriverEditorDialog(
            initial = driverEditor,
            onDismiss = {
                creatingDriver = false
                driverEditor = null
            },
            onSave = { id, name, phone, notes ->
                scope.launch {
                    val newId = withContext(Dispatchers.IO) { repo.upsertDriver(id, name, phone, notes) }
                    creatingDriver = false
                    driverEditor = null
                    selectedDriverId = newId
                    reload()
                }
            },
        )
    }

    completeRun?.let { run ->
        var arrivalText by remember(run.id) { mutableStateOf(formatEpochMs(System.currentTimeMillis())) }
        AlertDialog(
            onDismissRequest = { completeRun = null },
            title = { Text("Llegada a planta · traslado #${run.id}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    Text(
                        "Confirme fecha y hora de recepción en instalaciones. Se imputará flete " +
                            "(${formatMoney(run.freightCost)}) y grúa (${formatMoney(run.gruaCost)}) prorrateado " +
                            "entre los lotes del envío y quedará incorporado al costo de adquisición de cada lote. " +
                            "Los lotes pasan a ubicación Fábrica.",
                        style = AppTypography.BodySmall,
                    )
                    AppTextField(
                        value = arrivalText,
                        onValueChange = { arrivalText = it },
                        label = "Llegada (yyyy-MM-dd HH:mm)",
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val t = parseDateTime(arrivalText) ?: return@TextButton
                    scope.launch {
                        val r = withContext(Dispatchers.IO) { repo.completeProviderTransport(run.id, t) }
                        when (r) {
                            is InventoryRepository.TransportRunResult.Ok -> {
                                completeRun = null
                                reload()
                            }
                            is InventoryRepository.TransportRunResult.Err -> errorMsg = r.message
                        }
                    }
                }) { Text("Confirmar llegada") }
            },
            dismissButton = {
                TextButton(onClick = { completeRun = null }) { Text("Cancelar") }
            },
        )
    }
}

// =====================================================================================
// KPI
// =====================================================================================

private data class TransportKpis(
    val monthCount: Int,
    val prevMonthCount: Int,
    val inTransit: Int,
    val completedMonth: Int,
    val costMonth: Double,
    val lotsMonth: Int,
)

private fun computeTransportKpis(runs: List<ProviderTransportRun>): TransportKpis {
    val zone = ZoneId.systemDefault()
    val firstOfMonth = LocalDate.now().withDayOfMonth(1)
    val startThisMonth = firstOfMonth.atStartOfDay(zone).toInstant().toEpochMilli()
    val startNextMonth = firstOfMonth.plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val startPrevMonth = firstOfMonth.minusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()

    fun runEpoch(r: ProviderTransportRun): Long =
        r.departedAtEpochMs.takeIf { it > 0 } ?: r.createdAtEpochMs

    val thisMonth = runs.filter { val e = runEpoch(it); e in startThisMonth until startNextMonth }
    val prevMonth = runs.filter { val e = runEpoch(it); e in startPrevMonth until startThisMonth }

    return TransportKpis(
        monthCount = thisMonth.size,
        prevMonthCount = prevMonth.size,
        inTransit = runs.count { it.status == ProviderTransportRunStatus.IN_PROGRESS },
        completedMonth = thisMonth.count { it.status == ProviderTransportRunStatus.COMPLETED },
        costMonth = thisMonth.sumOf { it.freightCost + it.gruaCost },
        lotsMonth = thisMonth.sumOf { it.lots.size },
    )
}

// =====================================================================================
// Workflow shell + stepper
// =====================================================================================

@Composable
private fun WorkflowCard(
    currentStep: Int,
    onStepClick: (Int) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    SectionCard(
        modifier = modifier,
        title = "Nuevo traslado",
        subtitle = WIZARD_STEPS[currentStep],
    ) {
        WizardStepper(steps = WIZARD_STEPS, currentStep = currentStep, onStepClick = onStepClick)
        LinearProgressIndicator(
            progress = { (currentStep + 1) / WIZARD_STEPS.size.toFloat() },
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
            label = "wizardStep",
        ) { _ ->
            Box(modifier = Modifier.fillMaxWidth()) { content() }
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
            if (currentStep < WIZARD_STEPS.lastIndex) {
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
// Step 1 — Driver selection (cards)
// =====================================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepDrivers(
    drivers: List<Driver>,
    query: String,
    onQueryChange: (String) -> Unit,
    selectedId: Int?,
    onSelect: (Int) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Driver) -> Unit,
    onDelete: (Driver) -> Unit,
) {
    val filtered =
        drivers.filter {
            query.isBlank() ||
                it.name.contains(query, ignoreCase = true) ||
                (it.phone?.contains(query, ignoreCase = true) == true)
        }
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppSearchField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = "Buscar chofer por nombre o teléfono…",
                keyboardShortcutHint = null,
            )
            AppButton(
                text = "Agregar nuevo chofer",
                onClick = onAdd,
                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
        }
        if (drivers.isEmpty()) {
            EmptyState(
                title = "Sin choferes registrados",
                message = "Agregue al menos un chofer para iniciar traslados.",
            )
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                maxItemsInEachRow = 3,
            ) {
                filtered.forEach { driver ->
                    DriverCard(
                        modifier = Modifier.weight(1f),
                        driver = driver,
                        selected = driver.id == selectedId,
                        onSelect = { onSelect(driver.id) },
                        onEdit = { onEdit(driver) },
                        onDelete = { onDelete(driver) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DriverCard(
    driver: Driver,
    selected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
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
                    driver.name.firstOrNull()?.uppercase() ?: "?",
                    style = AppTypography.CardTitle,
                    color = accent,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    driver.name,
                    style = AppTypography.CardTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        driver.phone?.ifBlank { "Sin teléfono" } ?: "Sin teléfono",
                        style = AppTypography.BodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
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
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Edit, contentDescription = "Editar", modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "Borrar", modifier = Modifier.size(16.dp))
            }
        }
    }
}

// =====================================================================================
// Step 2 — Transport form
// =====================================================================================

@Composable
private fun StepTransport(
    vehiclePlate: String,
    onPlate: (String) -> Unit,
    freightText: String,
    onFreight: (String) -> Unit,
    gruaText: String,
    onGrua: (String) -> Unit,
    departedText: String,
    onDeparted: (String) -> Unit,
    expectedArrivalText: String,
    onExpectedArrival: (String) -> Unit,
    transportNotes: String,
    onNotes: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
        FieldWithIcon(Icons.Filled.DirectionsCar) {
            AppTextField(
                value = vehiclePlate,
                onValueChange = onPlate,
                modifier = Modifier.weight(1f),
                label = "Vehículo / Patente",
                placeholder = "Ej. 1234-ABC",
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            FieldWithIcon(Icons.Filled.Payments, modifier = Modifier.weight(1f)) {
                AppNumberField(
                    value = freightText,
                    onValueChange = onFreight,
                    modifier = Modifier.weight(1f),
                    label = "Costo transporte (Bs)",
                )
            }
            FieldWithIcon(Icons.Filled.Construction, modifier = Modifier.weight(1f)) {
                AppNumberField(
                    value = gruaText,
                    onValueChange = onGrua,
                    modifier = Modifier.weight(1f),
                    label = "Costo grúa (Bs)",
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            FieldWithIcon(Icons.Filled.CalendarMonth, modifier = Modifier.weight(1f)) {
                AppTextField(
                    value = departedText,
                    onValueChange = onDeparted,
                    modifier = Modifier.weight(1f),
                    label = "Fecha salida (yyyy-MM-dd HH:mm)",
                )
            }
            FieldWithIcon(Icons.Filled.CalendarMonth, modifier = Modifier.weight(1f)) {
                AppTextField(
                    value = expectedArrivalText,
                    onValueChange = onExpectedArrival,
                    modifier = Modifier.weight(1f),
                    label = "Llegada estimada (opcional)",
                )
            }
        }
        FieldWithIcon(Icons.AutoMirrored.Filled.Notes, alignTop = true) {
            AppTextArea(
                value = transportNotes,
                onValueChange = onNotes,
                modifier = Modifier.weight(1f),
                label = "Observaciones",
                minLines = 3,
                maxLines = 6,
            )
        }
        Text(
            "Flete y grúa no se prorratean al iniciar: al registrar la llegada se reparten entre los " +
                "lotes del envío (según cantidad de postes) y se suman al costo de adquisición de cada lote.",
            style = AppTypography.Caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// =====================================================================================
// Step 3 — Lot selection (cards)
// =====================================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepLots(
    lots: List<Product>,
    selectedIds: Set<Int>,
    onToggle: (Int, Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
        Text(
            "${lots.size} lote(s) en predio proveedor · ${selectedIds.size} seleccionado(s)",
            style = AppTypography.BodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (lots.isEmpty()) {
            EmptyState(
                title = "No hay lotes en proveedor",
                message = "En «Por etapa» elija ubicación «Proveedor» al crear el lote para poder trasladarlo.",
            )
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                maxItemsInEachRow = 3,
            ) {
                lots.forEach { lot ->
                    LotCard(
                        modifier = Modifier.weight(1f),
                        lot = lot,
                        selected = lot.id in selectedIds,
                        onToggle = { on -> onToggle(lot.id, on) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LotCard(
    lot: Product,
    selected: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectableCard(selected = selected, onClick = { onToggle(!selected) }, modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            val primary = MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(AppShapes.small)
                    .background(if (selected) primary else Color.Transparent)
                    .border(
                        width = 1.5.dp,
                        color = if (selected) primary else AppThemeState.semantic.border,
                        shape = AppShapes.small,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Text(
                lot.name,
                style = AppTypography.CardTitle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                lot.productLine.ifBlank { "Sin línea" },
                style = AppTypography.BodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            StatusChip(text = "${formatQty(lot.quantity)} disp.", kind = StatusKind.Info)
        }
    }
}

// =====================================================================================
// Step 4 — Confirm
// =====================================================================================

@Composable
private fun StepConfirm(
    driver: Driver?,
    vehiclePlate: String,
    freight: Double,
    grua: Double,
    departedText: String,
    expectedArrivalText: String,
    lots: List<Product>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Text(
            "Revise el traslado antes de iniciarlo.",
            style = AppTypography.BodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SummaryLine("Chofer", driver?.name ?: "—")
        SummaryLine("Vehículo", vehiclePlate.ifBlank { "—" })
        SummaryLine("Salida", departedText.ifBlank { "—" })
        SummaryLine("Llegada estimada", expectedArrivalText.ifBlank { "—" })
        SummaryLine("Costo transporte", "Bs ${formatMoney(freight)}")
        SummaryLine("Costo grúa", "Bs ${formatMoney(grua)}")
        HorizontalDivider(color = AppThemeState.semantic.border.copy(alpha = 0.4f))
        Text(
            "Lotes (${lots.size})",
            style = AppTypography.CardTitle,
            color = MaterialTheme.colorScheme.onSurface,
        )
        lots.forEach { lot ->
            Text(
                "· ${lot.name} — ${formatQty(lot.quantity)} postes",
                style = AppTypography.BodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (lots.isEmpty()) {
            Text(
                "Sin lotes seleccionados.",
                style = AppTypography.BodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

// =====================================================================================
// Right column — live summary + primary action bar
// =====================================================================================

@Composable
private fun TransferSummaryCard(
    driver: Driver?,
    vehiclePlate: String,
    freight: Double,
    grua: Double,
    total: Double,
    lots: List<Product>,
) {
    SectionCard(title = "Resumen del traslado", subtitle = "Cálculo en tiempo real") {
        SummaryLine("Chofer", driver?.name ?: "Sin seleccionar")
        SummaryLine("Vehículo", vehiclePlate.ifBlank { "—" })
        SummaryLine("Costo transporte", "Bs ${formatMoney(freight)}")
        SummaryLine("Costo grúa", "Bs ${formatMoney(grua)}")
        HorizontalDivider(color = AppThemeState.semantic.border.copy(alpha = 0.4f))
        Text(
            "Lotes seleccionados (${lots.size})",
            style = AppTypography.BodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (lots.isEmpty()) {
            Text(
                "Aún no hay lotes en el envío.",
                style = AppTypography.BodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                lots.take(6).forEach { lot ->
                    Text(
                        "· ${lot.name} (${formatQty(lot.quantity)})",
                        style = AppTypography.BodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (lots.size > 6) {
                    Text(
                        "+ ${lots.size - 6} más…",
                        style = AppTypography.Caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        HorizontalDivider(color = AppThemeState.semantic.border.copy(alpha = 0.4f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Costo total estimado",
                style = AppTypography.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Bs ${formatMoney(total)}",
                style = AppTypography.MetricMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun PrimaryActionBar(
    lotCount: Int,
    freight: Double,
    grua: Double,
    total: Double,
    enabled: Boolean,
    loading: Boolean,
    onStart: () -> Unit,
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            Text(
                "$lotCount lote(s) seleccionado(s)",
                style = AppTypography.BodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            SummaryLine("Costo transporte", "Bs ${formatMoney(freight)}")
            SummaryLine("Costo grúa", "Bs ${formatMoney(grua)}")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Total", style = AppTypography.CardTitle, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Bs ${formatMoney(total)}",
                    style = AppTypography.CardTitle.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            AppButton(
                text = if (loading) "Iniciando…" else "Iniciar Traslado",
                onClick = onStart,
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
                        Icon(Icons.Filled.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                },
            )
        }
    }
}

// =====================================================================================
// History — timeline cards
// =====================================================================================

@Composable
private fun TimelineTransferCard(
    run: ProviderTransportRun,
    isLast: Boolean,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
) {
    val badge = badgeFor(run.status)
    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        // Timeline indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(20.dp),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(statusKindColor(badge.kind)),
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .width(2.dp)
                        .height(96.dp)
                        .background(AppThemeState.semantic.border.copy(alpha = 0.5f)),
                )
            }
        }
        AppCard(modifier = Modifier.weight(1f), showHairlineBorder = true) {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "#${run.id} · ${run.driverName}",
                            style = AppTypography.CardTitle,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            run.vehiclePlate.ifBlank { "Sin patente" },
                            style = AppTypography.BodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    StatusDotBadge(badge.label, badge.kind)
                }
                FlowRowInfo(run)
                run.notes?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = AppTypography.BodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (run.status == ProviderTransportRunStatus.IN_PROGRESS) {
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                        AppButton(
                            text = "Registrar llegada",
                            onClick = onComplete,
                            leadingIcon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        )
                        AppOutlinedButton(text = "Cancelar envío", onClick = onCancel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowInfo(run: ProviderTransportRun) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        InfoPair("Salida", formatEpochMs(run.departedAtEpochMs))
        run.expectedArrivalEpochMs?.let { InfoPair("Est. llegada", formatEpochMs(it)) }
        run.arrivedAtEpochMs?.let { InfoPair("Llegada", formatEpochMs(it)) }
        InfoPair("Transporte", "Bs ${formatMoney(run.freightCost)}")
        InfoPair("Grúa", "Bs ${formatMoney(run.gruaCost)}")
        InfoPair("Lotes", run.lots.size.toString())
    }
}

@Composable
private fun InfoPair(label: String, value: String) {
    Column {
        Text(label, style = AppTypography.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = AppTypography.BodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

// =====================================================================================
// Status badge
// =====================================================================================

private data class BadgeSpec(val label: String, val kind: StatusKind)

private fun badgeFor(status: ProviderTransportRunStatus): BadgeSpec =
    when (status) {
        ProviderTransportRunStatus.IN_PROGRESS -> BadgeSpec("En tránsito", StatusKind.Warning)
        ProviderTransportRunStatus.COMPLETED -> BadgeSpec("Completado", StatusKind.Success)
        ProviderTransportRunStatus.CANCELLED -> BadgeSpec("Cancelado", StatusKind.Error)
    }

// =====================================================================================
// Driver editor dialog
// =====================================================================================

@Composable
private fun DriverEditorDialog(
    initial: Driver?,
    onDismiss: () -> Unit,
    onSave: (id: Int?, name: String, phone: String?, notes: String?) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var phone by remember { mutableStateOf(initial?.phone.orEmpty()) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nuevo chofer" else "Editar chofer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                AppTextField(value = name, onValueChange = { name = it }, label = "Nombre", modifier = Modifier.fillMaxWidth())
                AppTextField(value = phone, onValueChange = { phone = it }, label = "Teléfono (opcional)", modifier = Modifier.fillMaxWidth())
                AppTextField(value = notes, onValueChange = { notes = it }, label = "Notas", modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(initial?.id, name.trim(), phone.trim().ifBlank { null }, notes.trim().ifBlank { null })
                    }
                },
                enabled = name.isNotBlank(),
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
