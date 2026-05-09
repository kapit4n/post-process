package com.inventory.industry.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.inventory.industry.data.Driver
import com.inventory.industry.data.InventoryRepository
import com.inventory.industry.data.Product
import com.inventory.industry.data.ProviderTransportRun
import com.inventory.industry.data.ProviderTransportRunStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            eligibleLots =
                withContext(Dispatchers.IO) { repo.listProductsReadyPickupAtProvider() }
            runs = withContext(Dispatchers.IO) { repo.listProviderTransportRuns(80) }
        }
    }

    LaunchedEffect(Unit) {
        reload()
    }

    LaunchedEffect(drivers) {
        if (selectedDriverId == null && drivers.isNotEmpty()) {
            selectedDriverId = drivers.first().id
        }
    }

    Scaffold { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Traslados desde proveedor",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            item {
                Text(
                    "Registre choferes y vehículos. Desde predio proveedor inicie un envío: los lotes pasan a «En traslado». " +
                        "Al registrar la llegada, los importes de transporte y grúa del formulario se reparten entre los lotes " +
                        "del envío (según cantidad de postes) y se suman al costo de adquisición de cada lote, junto al pago al proveedor y a los traslados cargados en el lote.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            errorMsg?.let { msg ->
                item { Text(msg, color = MaterialTheme.colorScheme.error) }
            }
            deleteDriverError?.let { msg ->
                item {
                    Text(
                        msg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Registro de choferes",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "Nombre y contacto de quienes conducen los envíos desde proveedor.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    creatingDriver = true
                                    driverEditor = null
                                },
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Text(" Agregar", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Nombre", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            Text("Teléfono", modifier = Modifier.weight(0.8f), fontWeight = FontWeight.SemiBold)
                            Text("", modifier = Modifier.weight(0.4f))
                        }
                        HorizontalDivider()
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            drivers.forEach { d ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(d.name, modifier = Modifier.weight(1f), maxLines = 2)
                                    Text(d.phone.orEmpty(), modifier = Modifier.weight(0.8f), maxLines = 2)
                                    Row(
                                        modifier = Modifier.weight(0.4f),
                                        horizontalArrangement = Arrangement.End,
                                    ) {
                                        IconButton(onClick = {
                                            driverEditor = d
                                            creatingDriver = false
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Editar")
                                        }
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    deleteDriverError = null
                                                    val ok =
                                                        withContext(Dispatchers.IO) { repo.deleteDriver(d.id) }
                                                    if (ok) {
                                                        reload()
                                                        if (selectedDriverId == d.id) {
                                                            selectedDriverId =
                                                                drivers.firstOrNull { it.id != d.id }?.id
                                                        }
                                                    } else {
                                                        deleteDriverError =
                                                            "No se puede borrar: el chofer tiene traslados registrados."
                                                    }
                                                }
                                            },
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Borrar")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { HorizontalDivider() }
            item {
                Text(
                    "Nuevo traslado",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (drivers.isEmpty()) {
                item {
                    Text(
                        "Agregue al menos un chofer.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            } else {
                item {
                    CycleOrDropdownPicker(
                        items = drivers,
                        selected = drivers.firstOrNull { it.id == selectedDriverId },
                        onSelected = { selectedDriverId = it.id },
                        labelFor = { it.name + (it.phone?.let { p -> " · $p" } ?: "") },
                        placeholder = "Elegir chofer…",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                TextField(
                    value = vehiclePlate,
                    onValueChange = { vehiclePlate = it },
                    label = { Text("Patente / ID vehículo") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextField(
                        value = freightText,
                        onValueChange = { freightText = it },
                        label = { Text("Costo transporte (total viaje)") },
                        modifier = Modifier.weight(1f),
                    )
                    TextField(
                        value = gruaText,
                        onValueChange = { gruaText = it },
                        label = { Text("Costo grúa (total)") },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                Text(
                    "Estos totales no se prorratean al inicio: al «Registrar llegada» del envío, el sistema los reparte " +
                        "entre los lotes seleccionados (en proporción a la cantidad de postes) y los incorpora al costo de traslado del lote para valorizar adquisición.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                TextField(
                    value = departedText,
                    onValueChange = { departedText = it },
                    label = { Text("Salida (yyyy-MM-dd HH:mm)") },
                    isError = parseDateTime(departedText) == null && departedText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                TextField(
                    value = expectedArrivalText,
                    onValueChange = { expectedArrivalText = it },
                    label = { Text("Llegada estimada (opcional, mismo formato)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                TextField(
                    value = transportNotes,
                    onValueChange = { transportNotes = it },
                    label = { Text("Notas del envío") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Text(
                    "Lotes en predio proveedor",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            if (eligibleLots.isEmpty()) {
                item {
                    Text(
                        "No hay lotes en proveedor. En «Por etapa» elija ubicación «Proveedor» al crear el lote.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(eligibleLots, key = { it.id }) { p ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = p.id in selectedLotIds,
                            onCheckedChange = { on ->
                                selectedLotIds =
                                    if (on) {
                                        selectedLotIds + p.id
                                    } else {
                                        selectedLotIds - p.id
                                    }
                            },
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(p.name, fontWeight = FontWeight.Medium)
                            Text(
                                "${p.productLine} · ${formatQty(p.quantity)} postes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        errorMsg = null
                        val did = selectedDriverId ?: run {
                            errorMsg = "Elija un chofer."
                            return@Button
                        }
                        val dep = parseDateTime(departedText) ?: run {
                            errorMsg = "Fecha/hora de salida inválida."
                            return@Button
                        }
                        val freight = parseMoneyAmount(freightText) ?: run {
                            errorMsg = "Costo de transporte inválido."
                            return@Button
                        }
                        val grua = parseMoneyAmount(gruaText) ?: run {
                            errorMsg = "Costo de grúa inválido."
                            return@Button
                        }
                        val exp =
                            expectedArrivalText.trim().takeIf { it.isNotEmpty() }?.let {
                                parseDateTime(it)
                            }
                        if (expectedArrivalText.isNotBlank() && exp == null) {
                            errorMsg = "Fecha de llegada estimada inválida."
                            return@Button
                        }
                        if (selectedLotIds.isEmpty()) {
                            errorMsg = "Seleccione al menos un lote."
                            return@Button
                        }
                        scope.launch {
                            val r =
                                withContext(Dispatchers.IO) {
                                    repo.startProviderTransport(
                                        driverId = did,
                                        vehiclePlate = vehiclePlate,
                                        freightCost = freight,
                                        gruaCost = grua,
                                        departedAtEpochMs = dep,
                                        expectedArrivalEpochMs = exp,
                                        productIds = selectedLotIds.toList(),
                                        notes = transportNotes.trim().ifBlank { null },
                                    )
                                }
                            when (r) {
                                is InventoryRepository.TransportRunResult.Ok -> {
                                    selectedLotIds = emptySet()
                                    vehiclePlate = ""
                                    freightText = "0"
                                    gruaText = "0"
                                    expectedArrivalText = ""
                                    transportNotes = ""
                                    reload()
                                }
                                is InventoryRepository.TransportRunResult.Err -> errorMsg = r.message
                            }
                        }
                    },
                    enabled = drivers.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Iniciar traslado")
                }
            }
            item { HorizontalDivider() }
            item {
                Text(
                    "Envíos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(runs, key = { it.id }) { run ->
                RunCard(
                    run = run,
                    onComplete = { completeRun = run },
                    onCancel = {
                        scope.launch {
                            val r =
                                withContext(Dispatchers.IO) { repo.cancelProviderTransport(run.id) }
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

    if (creatingDriver || driverEditor != null) {
        DriverEditorDialog(
            initial = driverEditor,
            onDismiss = {
                creatingDriver = false
                driverEditor = null
            },
            onSave = { id, name, phone, notes ->
                scope.launch {
                    withContext(Dispatchers.IO) { repo.upsertDriver(id, name, phone, notes) }
                    creatingDriver = false
                    driverEditor = null
                    reload()
                }
            },
        )
    }

    completeRun?.let { run ->
        var arrivalText by remember(run.id) {
            mutableStateOf(formatEpochMs(System.currentTimeMillis()))
        }
        AlertDialog(
            onDismissRequest = { completeRun = null },
            title = { Text("Llegada a planta · traslado #${run.id}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Confirme fecha y hora de recepción en instalaciones. " +
                            "Se imputará flete (${formatMoney(run.freightCost)}) y grúa " +
                            "(${formatMoney(run.gruaCost)}) prorrateado entre los lotes del envío " +
                            "y quedará incorporado al costo de adquisición (traslado) de cada lote. " +
                            "Los lotes pasan a ubicación Fábrica.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TextField(
                        value = arrivalText,
                        onValueChange = { arrivalText = it },
                        label = { Text("Llegada (yyyy-MM-dd HH:mm)") },
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val t = parseDateTime(arrivalText) ?: return@Button
                        scope.launch {
                            val r =
                                withContext(Dispatchers.IO) {
                                    repo.completeProviderTransport(run.id, t)
                                }
                            when (r) {
                                is InventoryRepository.TransportRunResult.Ok -> {
                                    completeRun = null
                                    reload()
                                }
                                is InventoryRepository.TransportRunResult.Err -> errorMsg = r.message
                            }
                        }
                    },
                ) {
                    Text("Confirmar llegada")
                }
            },
            dismissButton = {
                TextButton(onClick = { completeRun = null }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun RunCard(
    run: ProviderTransportRun,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "#${run.id} · ${run.driverName} · ${run.vehiclePlate} · ${run.status.name}",
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Salida: ${formatEpochMs(run.departedAtEpochMs)}" +
                    (run.expectedArrivalEpochMs?.let { " · Est. llegada: ${formatEpochMs(it)}" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
            )
            run.arrivedAtEpochMs?.let {
                Text("Llegada: ${formatEpochMs(it)}", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                "Transporte ${formatMoney(run.freightCost)} · Grua ${formatMoney(run.gruaCost)}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text("Lotes:", style = MaterialTheme.typography.labelSmall)
            run.lots.forEach { l ->
                Text(
                    "· ${l.productName} — ${formatQty(l.quantity)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            run.notes?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            if (run.status == ProviderTransportRunStatus.IN_PROGRESS) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onComplete) { Text("Registrar llegada") }
                    OutlinedButton(onClick = onCancel) { Text("Cancelar envío") }
                }
            }
        }
    }
}

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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Teléfono (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(initial?.id, name.trim(), phone.trim().ifBlank { null }, notes.trim().ifBlank { null })
                    }
                },
                enabled = name.isNotBlank(),
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
