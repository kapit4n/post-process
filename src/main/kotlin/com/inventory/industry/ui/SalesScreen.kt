package com.inventory.industry.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inventory.industry.data.Client
import com.inventory.industry.data.InventoryRepository
import com.inventory.industry.data.Product
import com.inventory.industry.data.SaleRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SalesScreen(repo: InventoryRepository) {
    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var sellable by remember { mutableStateOf<List<Product>>(emptyList()) }
    var recent by remember { mutableStateOf<List<SaleRecord>>(emptyList()) }
    var clientId by remember { mutableStateOf<Int?>(null) }
    var productId by remember { mutableStateOf<Int?>(null) }
    var qtyText by remember { mutableStateOf("1") }
    var totalText by remember { mutableStateOf("") }
    var whenText by remember { mutableStateOf(formatEpochMs(System.currentTimeMillis())) }
    var saleNotes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

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

    val selectedClient = clients.firstOrNull { it.id == clientId }
    val selectedProduct = sellable.firstOrNull { it.id == productId }

    LaunchedEffect(selectedProduct?.id) {
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

    Scaffold { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Registrar venta",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Venta de postes en estado Terminado (OK) o lotes fallados a precio de saldo. " +
                    "Se registra el precio real cobrado para contabilidad.",
                style = MaterialTheme.typography.bodyMedium,
            )

            Text("Cliente", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(
                onClick = {
                    if (clients.isEmpty()) return@OutlinedButton
                    val idx = clients.indexOfFirst { it.id == clientId }.let { if (it < 0) 0 else it }
                    clientId = clients[(idx + 1) % clients.size].id
                },
                enabled = clients.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(selectedClient?.name ?: if (clients.isEmpty()) "Cree clientes primero" else "Elegir cliente…")
            }

            Text("Lote a vender", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(
                onClick = {
                    if (sellable.isEmpty()) return@OutlinedButton
                    val idx = sellable.indexOfFirst { it.id == productId }.let { if (it < 0) 0 else it }
                    productId = sellable[(idx + 1) % sellable.size].id
                },
                enabled = sellable.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    selectedProduct?.let { p ->
                        "${p.name} · ${p.stage.shortCode} · ${if (p.isFailed) "Saldo" else "OK"} · disp. ${formatQty(p.quantity)}"
                    } ?: if (sellable.isEmpty()) {
                        "No hay lotes vendibles"
                    } else {
                        "Elegir lote…"
                    },
                )
            }

            selectedProduct?.let { p ->
                Text(
                    "Precio referencia: ${p.effectiveSalePrice()?.let { formatMoney(it) } ?: "—"} / poste",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text("Cantidad") },
                    modifier = Modifier.weight(1f),
                )
                TextField(
                    value = totalText,
                    onValueChange = { totalText = it },
                    label = { Text("Total cobrado") },
                    modifier = Modifier.weight(1f),
                )
            }
            TextField(
                value = whenText,
                onValueChange = { whenText = it },
                label = { Text("Fecha / hora venta (yyyy-MM-dd HH:mm)") },
                modifier = Modifier.fillMaxWidth(),
            )
            TextField(
                value = saleNotes,
                onValueChange = { saleNotes = it },
                label = { Text("Notas (opcional)") },
                modifier = Modifier.fillMaxWidth(),
            )

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = {
                    error = null
                    val cid = clientId ?: run {
                        error = "Seleccione un cliente."
                        return@Button
                    }
                    val pid = productId ?: run {
                        error = "Seleccione un lote."
                        return@Button
                    }
                    val q = qtyText.toDoubleOrNull() ?: run {
                        error = "Cantidad inválida."
                        return@Button
                    }
                    val total = totalText.toDoubleOrNull() ?: run {
                        error = "Monto total inválido."
                        return@Button
                    }
                    val whenMs = parseDateTime(whenText) ?: run {
                        error = "Fecha u hora inválida."
                        return@Button
                    }
                    scope.launch {
                        val result =
                            withContext(Dispatchers.IO) {
                                repo.recordSale(
                                    productId = pid,
                                    clientId = cid,
                                    quantitySold = q,
                                    totalAmount = total,
                                    soldAtEpochMs = whenMs,
                                    notes = saleNotes.trim().ifBlank { null },
                                )
                            }
                        when (result) {
                            is InventoryRepository.SaleRecordingResult.Ok -> {
                                saleNotes = ""
                                reloadLists()
                            }
                            is InventoryRepository.SaleRecordingResult.Err -> error = result.message
                        }
                    }
                },
                enabled = clients.isNotEmpty() && sellable.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Confirmar venta")
            }

            HorizontalDivider()
            Text("Ventas recientes", style = MaterialTheme.typography.titleMedium)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(recent, key = { it.id }) { s ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "${formatEpochMs(s.soldAtEpochMs)} · ${s.clientName}",
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "${s.snapshotProductName} · ${formatQty(s.quantitySold)} postes · ${formatMoney(s.totalAmount)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                "${s.snapshotStage.shortCode} · ${if (s.snapshotWasFailed) "Saldo" else "OK"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
