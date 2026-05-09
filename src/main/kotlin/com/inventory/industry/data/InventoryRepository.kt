package com.inventory.industry.data

import com.inventory.industry.domain.PoleStorageLocation
import com.inventory.industry.domain.ProductStage
import java.time.Instant
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.leftJoin
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.comparisons.compareBy
import kotlin.math.abs

class InventoryRepository {
    fun listCatalogProducts(): List<CatalogProduct> =
        transaction {
            CatalogProductsTable
                .selectAll()
                .orderBy(CatalogProductsTable.name to SortOrder.ASC)
                .map {
                    CatalogProduct(
                        id = it[CatalogProductsTable.id],
                        name = it[CatalogProductsTable.name],
                        productLine = it[CatalogProductsTable.productLine],
                        description = it[CatalogProductsTable.description],
                        createdAtEpochMs = it[CatalogProductsTable.createdAtEpochMs],
                    )
                }
        }

    fun upsertCatalogProduct(
        id: Int?,
        name: String,
        productLine: String,
        description: String?,
    ): Int =
        transaction {
            val now = System.currentTimeMillis()
            if (id == null) {
                CatalogProductsTable.insert {
                    it[CatalogProductsTable.name] = name
                    it[CatalogProductsTable.productLine] = productLine
                    it[CatalogProductsTable.description] = description
                    it[CatalogProductsTable.createdAtEpochMs] = now
                }[CatalogProductsTable.id]
            } else {
                CatalogProductsTable.update({ CatalogProductsTable.id eq id }) {
                    it[CatalogProductsTable.name] = name
                    it[CatalogProductsTable.productLine] = productLine
                    it[CatalogProductsTable.description] = description
                }
                id
            }
        }

    fun deleteCatalogProduct(id: Int) {
        transaction {
            ProductsTable.update({ ProductsTable.catalogProductId eq id }) {
                it[ProductsTable.catalogProductId] = null
            }
            CatalogProductsTable.deleteWhere { CatalogProductsTable.id eq id }
        }
    }

    fun listProducts(): List<Product> =
        transaction {
            (ProductsTable leftJoin PoleProvidersTable)
                .selectAll()
                .orderBy(ProductsTable.name to SortOrder.ASC)
                .map { it.toProduct() }
        }

    fun listProducts(stage: ProductStage): List<Product> =
        transaction {
            (ProductsTable leftJoin PoleProvidersTable)
                .selectAll()
                .where { ProductsTable.stage eq stage.name }
                .orderBy(ProductsTable.name to SortOrder.ASC)
                .map { it.toProduct() }
        }

    fun getProduct(id: Int): Product? =
        transaction {
            (ProductsTable leftJoin PoleProvidersTable)
                .selectAll()
                .where { ProductsTable.id eq id }
                .singleOrNull()
                ?.toProduct()
        }

    /** Lotes que se pueden vender: terminados OK o cualquier lote fallado con stock. */
    fun listSellableProducts(): List<Product> =
        listProducts().filter { it.isSellable() && it.quantity > 1e-6 }

    fun inventoryFlowSummary(): InventoryFlowSummary {
        val products = listProducts()
        val perStage =
            ProductStage.entries.map { st ->
                val list = products.filter { it.stage == st }
                StageInventoryRow(
                    stage = st,
                    totalPoles = list.sumOf { it.quantity },
                    lotCount = list.size,
                    okPoles = list.filter { !it.isFailed }.sumOf { it.quantity },
                    failedPoles = list.filter { it.isFailed }.sumOf { it.quantity },
                )
            }
        val inProcess =
            listOf(ProductStage.CRUDO, ProductStage.DESCORTEZADO, ProductStage.TRATADO).sumOf { st ->
                products.filter { it.stage == st && !it.isFailed }.sumOf { it.quantity }
            }
        val finished =
            products
                .filter { it.stage == ProductStage.TERMINADO && !it.isFailed }
                .sumOf { it.quantity }
        val salvage = products.filter { it.isFailed }.sumOf { it.quantity }
        return InventoryFlowSummary(perStage, inProcess, finished, salvage)
    }

    /** Actividad reciente combinada (ventas, transformaciones, traslados) para el dashboard. */
    fun recentDashboardActivity(limit: Int = 16): List<DashboardActivityEntry> {
        val sales = listSales(40)
        val transforms = listTransformations(40)
        val transports = listProviderTransportRuns(25)
        val events = mutableListOf<DashboardActivityEntry>()
        sales.forEach { s ->
            events.add(
                DashboardActivityEntry(
                    kind = DashboardActivityKind.Sale,
                    title = "Venta · ${s.snapshotProductName}",
                    subtitle =
                        "${s.clientName} · ${fmtDashboardQty(s.quantitySold)} u. · ${fmtDashboardMoney(s.totalAmount)}",
                    epochMs = s.soldAtEpochMs,
                ),
            )
        }
        transforms.forEach { t ->
            val whenMs = t.processedAtEpochMs.coerceAtLeast(t.createdAtEpochMs)
            val status =
                if (t.processingStatus == TransformationProcessingStatus.IN_PROGRESS) {
                    "En curso"
                } else {
                    "Completado"
                }
            events.add(
                DashboardActivityEntry(
                    kind = DashboardActivityKind.Transformation,
                    title = "Producción ${t.fromStage.shortCode} → ${t.toStage.shortCode} · $status",
                    subtitle =
                        "Entrada ${fmtDashboardQty(t.totalInput)} postes · Costo ${fmtDashboardMoney(t.totalCost)}",
                    epochMs = whenMs,
                ),
            )
        }
        transports.forEach { r ->
            val whenMs =
                listOfNotNull(
                    r.arrivedAtEpochMs,
                    r.departedAtEpochMs,
                    r.createdAtEpochMs,
                ).maxOrNull() ?: r.createdAtEpochMs
            events.add(
                DashboardActivityEntry(
                    kind = DashboardActivityKind.Transport,
                    title = "Traslado · ${r.driverName} (${r.vehiclePlate})",
                    subtitle =
                        "${r.lots.size} lote(s) · ${r.status.name.lowercase().replace('_', ' ')}",
                    epochMs = whenMs,
                ),
            )
        }
        return events.sortedByDescending { it.epochMs }.take(limit)
    }

    private fun fmtDashboardQty(value: Double): String =
        if (abs(value - value.toLong()) < 1e-6) value.toLong().toString() else "%.2f".format(value)

    private fun fmtDashboardMoney(value: Double): String = "%.2f".format(value)

    fun listPoleProviders(): List<PoleProvider> =
        transaction {
            PoleProvidersTable
                .selectAll()
                .orderBy(PoleProvidersTable.name to SortOrder.ASC)
                .map {
                    PoleProvider(
                        id = it[PoleProvidersTable.id],
                        name = it[PoleProvidersTable.name],
                        contact = it[PoleProvidersTable.contact],
                        notes = it[PoleProvidersTable.notes],
                        createdAtEpochMs = it[PoleProvidersTable.createdAtEpochMs],
                    )
                }
        }

    fun upsertPoleProvider(
        id: Int?,
        name: String,
        contact: String?,
        notes: String?,
    ): Int =
        transaction {
            val now = System.currentTimeMillis()
            if (id == null) {
                PoleProvidersTable.insert {
                    it[PoleProvidersTable.name] = name
                    it[PoleProvidersTable.contact] = contact
                    it[PoleProvidersTable.notes] = notes
                    it[PoleProvidersTable.createdAtEpochMs] = now
                }[PoleProvidersTable.id]
            } else {
                PoleProvidersTable.update({ PoleProvidersTable.id eq id }) {
                    it[PoleProvidersTable.name] = name
                    it[PoleProvidersTable.contact] = contact
                    it[PoleProvidersTable.notes] = notes
                }
                id
            }
        }

    fun deletePoleProvider(id: Int) {
        transaction {
            ProductsTable.update({ ProductsTable.providerId eq id }) {
                it[ProductsTable.providerId] = null
            }
            PoleProvidersTable.deleteWhere { PoleProvidersTable.id eq id }
        }
    }

    sealed class TransportRunResult {
        data class Ok(val runId: Int) : TransportRunResult()

        data class Err(val message: String) : TransportRunResult()
    }

    fun listDrivers(): List<Driver> =
        transaction {
            DriversTable
                .selectAll()
                .orderBy(DriversTable.name to SortOrder.ASC)
                .map {
                    Driver(
                        id = it[DriversTable.id],
                        name = it[DriversTable.name],
                        phone = it[DriversTable.phone],
                        notes = it[DriversTable.notes],
                        createdAtEpochMs = it[DriversTable.createdAtEpochMs],
                    )
                }
        }

    fun upsertDriver(
        id: Int?,
        name: String,
        phone: String?,
        notes: String?,
    ): Int =
        transaction {
            val now = System.currentTimeMillis()
            if (id == null) {
                DriversTable.insert {
                    it[DriversTable.name] = name.trim()
                    it[DriversTable.phone] = phone?.trim()?.ifBlank { null }
                    it[DriversTable.notes] = notes?.trim()?.ifBlank { null }
                    it[DriversTable.createdAtEpochMs] = now
                }[DriversTable.id]
            } else {
                DriversTable.update({ DriversTable.id eq id }) {
                    it[DriversTable.name] = name.trim()
                    it[DriversTable.phone] = phone?.trim()?.ifBlank { null }
                    it[DriversTable.notes] = notes?.trim()?.ifBlank { null }
                }
                id
            }
        }

    fun deleteDriver(id: Int): Boolean =
        transaction {
            val used =
                ProviderTransportRunsTable
                    .selectAll()
                    .where { ProviderTransportRunsTable.driverId eq id }
                    .count()
            if (used > 0L) return@transaction false
            DriversTable.deleteWhere { DriversTable.id eq id }
            true
        }

    /** Lotes en predio proveedor listos para iniciar un traslado a planta. */
    fun listProductsReadyPickupAtProvider(): List<Product> =
        listProducts()
            .filter {
                !it.isFailed && it.acquisitionStorageLocation == PoleStorageLocation.EN_PROVEEDOR
            }
            .sortedBy { it.name }

    fun startProviderTransport(
        driverId: Int,
        vehiclePlate: String,
        freightCost: Double,
        gruaCost: Double,
        departedAtEpochMs: Long,
        expectedArrivalEpochMs: Long?,
        productIds: List<Int>,
        notes: String?,
    ): TransportRunResult =
        transaction {
            val plate = vehiclePlate.trim()
            if (plate.isEmpty()) {
                return@transaction TransportRunResult.Err("Indique patente o identificación del vehículo.")
            }
            if (freightCost < -1e-9 || gruaCost < -1e-9) {
                return@transaction TransportRunResult.Err("Los costos no pueden ser negativos.")
            }
            val distinctIds = productIds.distinct().filter { it > 0 }
            if (distinctIds.isEmpty()) {
                return@transaction TransportRunResult.Err(
                    "Seleccione al menos un lote ubicado en predio proveedor.",
                )
            }

            DriversTable
                .selectAll()
                .where { DriversTable.id eq driverId }
                .singleOrNull()
                ?: return@transaction TransportRunResult.Err("Chofer no encontrado.")

            for (pid in distinctIds) {
                val otherRuns =
                    ProviderTransportRunProductsTable
                        .selectAll()
                        .where { ProviderTransportRunProductsTable.productId eq pid }
                for (link in otherRuns) {
                    val rid = link[ProviderTransportRunProductsTable.transportRunId]
                    val stRow =
                        ProviderTransportRunsTable
                            .selectAll()
                            .where { ProviderTransportRunsTable.id eq rid }
                            .singleOrNull() ?: continue
                    if (
                        ProviderTransportRunStatus.fromDb(stRow[ProviderTransportRunsTable.status]) ==
                        ProviderTransportRunStatus.IN_PROGRESS
                    ) {
                        return@transaction TransportRunResult.Err(
                            "El lote #$pid ya figura en otro traslado en curso.",
                        )
                    }
                }
            }

            for (pid in distinctIds) {
                val prow =
                    ProductsTable
                        .selectAll()
                        .where { ProductsTable.id eq pid }
                        .singleOrNull()
                        ?: return@transaction TransportRunResult.Err("Lote #$pid no existe.")
                if (prow[ProductsTable.isFailed]) {
                    return@transaction TransportRunResult.Err(
                        "El lote '${prow[ProductsTable.name]}' está en saldo / fallado.",
                    )
                }
                val loc = PoleStorageLocation.fromDb(prow[ProductsTable.acquisitionStorageLocation])
                if (loc != PoleStorageLocation.EN_PROVEEDOR) {
                    return@transaction TransportRunResult.Err(
                        "El lote '${prow[ProductsTable.name]}' debe estar en predio proveedor " +
                            "(ubicación actual: ${loc.shortLabel}).",
                    )
                }
            }

            val now = System.currentTimeMillis()
            val runId =
                ProviderTransportRunsTable.insert {
                    it[ProviderTransportRunsTable.driverId] = driverId
                    it[ProviderTransportRunsTable.vehiclePlate] = plate
                    it[ProviderTransportRunsTable.freightCost] = freightCost
                    it[ProviderTransportRunsTable.gruaCost] = gruaCost
                    it[ProviderTransportRunsTable.departedAtEpochMs] = departedAtEpochMs
                    it[ProviderTransportRunsTable.expectedArrivalEpochMs] = expectedArrivalEpochMs
                    it[ProviderTransportRunsTable.arrivedAtEpochMs] = null
                    it[ProviderTransportRunsTable.status] = ProviderTransportRunStatus.IN_PROGRESS.name
                    it[ProviderTransportRunsTable.notes] = notes?.trim()?.ifBlank { null }
                    it[ProviderTransportRunsTable.createdAtEpochMs] = now
                }[ProviderTransportRunsTable.id]

            for (pid in distinctIds) {
                ProviderTransportRunProductsTable.insert {
                    it[ProviderTransportRunProductsTable.transportRunId] = runId
                    it[ProviderTransportRunProductsTable.productId] = pid
                }
                ProductsTable.update({ ProductsTable.id eq pid }) {
                    it[ProductsTable.acquisitionStorageLocation] = PoleStorageLocation.EN_TRANSITO.name
                }
            }

            TransportRunResult.Ok(runId)
        }

    fun completeProviderTransport(
        runId: Int,
        arrivedAtEpochMs: Long,
    ): TransportRunResult =
        transaction {
            val runRow =
                ProviderTransportRunsTable
                    .selectAll()
                    .where { ProviderTransportRunsTable.id eq runId }
                    .singleOrNull()
                    ?: return@transaction TransportRunResult.Err("Traslado no encontrado.")
            if (
                ProviderTransportRunStatus.fromDb(runRow[ProviderTransportRunsTable.status]) !=
                ProviderTransportRunStatus.IN_PROGRESS
            ) {
                return@transaction TransportRunResult.Err("Este traslado no está en curso.")
            }
            val freightTotal = runRow[ProviderTransportRunsTable.freightCost]
            val gruaTotal = runRow[ProviderTransportRunsTable.gruaCost]

            val links =
                ProviderTransportRunProductsTable
                    .selectAll()
                    .where { ProviderTransportRunProductsTable.transportRunId eq runId }
            val productIds = links.map { it[ProviderTransportRunProductsTable.productId] }
            if (productIds.isEmpty()) {
                return@transaction TransportRunResult.Err("El traslado no tiene lotes asociados.")
            }

            val productRows =
                productIds.map { pid ->
                    ProductsTable
                        .selectAll()
                        .where { ProductsTable.id eq pid }
                        .singleOrNull()
                        ?: return@transaction TransportRunResult.Err("Lote #$pid no existe.")
                }
            val totalQty = productRows.sumOf { it[ProductsTable.quantity] }
            if (totalQty <= 1e-12) {
                return@transaction TransportRunResult.Err("Cantidad total inválida.")
            }

            val now = System.currentTimeMillis()
            val labelFreight = "Flete (traslado #${runId})"
            val labelGrua = "Grua (traslado #${runId})"

            for (row in productRows) {
                val pid = row[ProductsTable.id]
                val q = row[ProductsTable.quantity]
                val share = q / totalQty
                val fShare = freightTotal * share
                val gShare = gruaTotal * share
                if (fShare > 1e-9) {
                    AcquisitionTransportCostsTable.insert {
                        it[AcquisitionTransportCostsTable.productId] = pid
                        it[AcquisitionTransportCostsTable.label] = labelFreight
                        it[AcquisitionTransportCostsTable.lineCost] = fShare
                        it[AcquisitionTransportCostsTable.notes] = null
                        it[AcquisitionTransportCostsTable.createdAtEpochMs] = now
                    }
                }
                if (gShare > 1e-9) {
                    AcquisitionTransportCostsTable.insert {
                        it[AcquisitionTransportCostsTable.productId] = pid
                        it[AcquisitionTransportCostsTable.label] = labelGrua
                        it[AcquisitionTransportCostsTable.lineCost] = gShare
                        it[AcquisitionTransportCostsTable.notes] = null
                        it[AcquisitionTransportCostsTable.createdAtEpochMs] = now
                    }
                }
                ProductsTable.update({ ProductsTable.id eq pid }) {
                    it[ProductsTable.acquisitionStorageLocation] = PoleStorageLocation.FABRICA.name
                }
            }

            ProviderTransportRunsTable.update({ ProviderTransportRunsTable.id eq runId }) {
                it[ProviderTransportRunsTable.status] = ProviderTransportRunStatus.COMPLETED.name
                it[ProviderTransportRunsTable.arrivedAtEpochMs] = arrivedAtEpochMs
            }

            TransportRunResult.Ok(runId)
        }

    fun cancelProviderTransport(runId: Int): TransportRunResult =
        transaction {
            val runRow =
                ProviderTransportRunsTable
                    .selectAll()
                    .where { ProviderTransportRunsTable.id eq runId }
                    .singleOrNull()
                    ?: return@transaction TransportRunResult.Err("Traslado no encontrado.")
            if (
                ProviderTransportRunStatus.fromDb(runRow[ProviderTransportRunsTable.status]) !=
                ProviderTransportRunStatus.IN_PROGRESS
            ) {
                return@transaction TransportRunResult.Err("Sólo se cancelan traslados en curso.")
            }
            val pids =
                ProviderTransportRunProductsTable
                    .selectAll()
                    .where { ProviderTransportRunProductsTable.transportRunId eq runId }
                    .map { it[ProviderTransportRunProductsTable.productId] }
            for (pid in pids) {
                ProductsTable.update({ ProductsTable.id eq pid }) {
                    it[ProductsTable.acquisitionStorageLocation] = PoleStorageLocation.EN_PROVEEDOR.name
                }
            }
            ProviderTransportRunsTable.deleteWhere { ProviderTransportRunsTable.id eq runId }
            TransportRunResult.Ok(runId)
        }

    fun listProviderTransportRuns(limit: Int = 100): List<ProviderTransportRun> =
        transaction {
            val runRows =
                ProviderTransportRunsTable
                    .selectAll()
                    .orderBy(ProviderTransportRunsTable.departedAtEpochMs to SortOrder.DESC)
                    .limit(limit)
                    .toList()
            if (runRows.isEmpty()) return@transaction emptyList()

            val driverIds = runRows.map { it[ProviderTransportRunsTable.driverId] }.distinct()
            val driversById =
                DriversTable
                    .selectAll()
                    .where { DriversTable.id inList driverIds }
                    .associate { it[DriversTable.id] to it[DriversTable.name] }

            val runIds = runRows.map { it[ProviderTransportRunsTable.id] }
            val lotRows =
                ProviderTransportRunProductsTable
                    .selectAll()
                    .where { ProviderTransportRunProductsTable.transportRunId inList runIds }
            val allPids = lotRows.map { it[ProviderTransportRunProductsTable.productId] }.distinct()
            val productInfo: Map<Int, Pair<String, Double>> =
                if (allPids.isEmpty()) {
                    emptyMap()
                } else {
                    ProductsTable
                        .selectAll()
                        .where { ProductsTable.id inList allPids }
                        .associate {
                            it[ProductsTable.id] to
                                (it[ProductsTable.name] to it[ProductsTable.quantity])
                        }
                }
            val lotsByRunId =
                lotRows
                    .groupBy { it[ProviderTransportRunProductsTable.transportRunId] }
                    .mapValues { (_, lst) ->
                        lst.mapNotNull { lr ->
                            val pid = lr[ProviderTransportRunProductsTable.productId]
                            val p = productInfo[pid] ?: return@mapNotNull null
                            ProviderTransportRunLot(
                                productId = pid,
                                productName = p.first,
                                quantity = p.second,
                            )
                        }
                    }

            runRows.map { r ->
                val id = r[ProviderTransportRunsTable.id]
                ProviderTransportRun(
                    id = id,
                    driverId = r[ProviderTransportRunsTable.driverId],
                    driverName = driversById[r[ProviderTransportRunsTable.driverId]] ?: "—",
                    vehiclePlate = r[ProviderTransportRunsTable.vehiclePlate],
                    freightCost = r[ProviderTransportRunsTable.freightCost],
                    gruaCost = r[ProviderTransportRunsTable.gruaCost],
                    departedAtEpochMs = r[ProviderTransportRunsTable.departedAtEpochMs],
                    expectedArrivalEpochMs = r[ProviderTransportRunsTable.expectedArrivalEpochMs],
                    arrivedAtEpochMs = r[ProviderTransportRunsTable.arrivedAtEpochMs],
                    status = ProviderTransportRunStatus.fromDb(r[ProviderTransportRunsTable.status]),
                    notes = r[ProviderTransportRunsTable.notes],
                    createdAtEpochMs = r[ProviderTransportRunsTable.createdAtEpochMs],
                    lots = lotsByRunId[id].orEmpty(),
                )
            }
        }

    /**
     * Para cada lote en un envío **en curso**: datos de salida, ETA a planta y referencia del traslado.
     */
    fun inboundEtaByProductId(productIds: Collection<Int>): Map<Int, PoleInboundEta> =
        transaction {
            val ids = productIds.distinct().filter { it > 0 }
            if (ids.isEmpty()) return@transaction emptyMap()
            val links =
                ProviderTransportRunProductsTable
                    .selectAll()
                    .where { ProviderTransportRunProductsTable.productId inList ids }
            if (!links.any()) return@transaction emptyMap()

            val runIds = links.map { it[ProviderTransportRunProductsTable.transportRunId] }.distinct()
            val activeRuns =
                ProviderTransportRunsTable
                    .selectAll()
                    .where {
                        (ProviderTransportRunsTable.id inList runIds) and
                            (ProviderTransportRunsTable.status eq ProviderTransportRunStatus.IN_PROGRESS.name)
                    }
                    .associateBy { it[ProviderTransportRunsTable.id] }
            if (activeRuns.isEmpty()) return@transaction emptyMap()

            val driverIds =
                activeRuns.values.map { it[ProviderTransportRunsTable.driverId] }.distinct()
            val driverNames =
                DriversTable
                    .selectAll()
                    .where { DriversTable.id inList driverIds }
                    .associate { it[DriversTable.id] to it[DriversTable.name] }

            val out = mutableMapOf<Int, PoleInboundEta>()
            for (link in links) {
                val pid = link[ProviderTransportRunProductsTable.productId]
                val rid = link[ProviderTransportRunProductsTable.transportRunId]
                val run = activeRuns[rid] ?: continue
                out[pid] =
                    PoleInboundEta(
                        transportRunId = rid,
                        driverName = driverNames[run[ProviderTransportRunsTable.driverId]] ?: "—",
                        vehiclePlate = run[ProviderTransportRunsTable.vehiclePlate],
                        departedAtEpochMs = run[ProviderTransportRunsTable.departedAtEpochMs],
                        expectedArrivalEpochMs = run[ProviderTransportRunsTable.expectedArrivalEpochMs],
                    )
            }
            out
        }

    fun listClients(): List<Client> =
        transaction {
            ClientsTable
                .selectAll()
                .orderBy(ClientsTable.name to SortOrder.ASC)
                .map {
                    Client(
                        id = it[ClientsTable.id],
                        name = it[ClientsTable.name],
                        contact = it[ClientsTable.contact],
                        notes = it[ClientsTable.notes],
                        createdAtEpochMs = it[ClientsTable.createdAtEpochMs],
                    )
                }
        }

    fun upsertClient(
        id: Int?,
        name: String,
        contact: String?,
        notes: String?,
    ): Int =
        transaction {
            val now = System.currentTimeMillis()
            if (id == null) {
                ClientsTable.insert {
                    it[ClientsTable.name] = name
                    it[ClientsTable.contact] = contact
                    it[ClientsTable.notes] = notes
                    it[ClientsTable.createdAtEpochMs] = now
                }[ClientsTable.id]
            } else {
                ClientsTable.update({ ClientsTable.id eq id }) {
                    it[ClientsTable.name] = name
                    it[ClientsTable.contact] = contact
                    it[ClientsTable.notes] = notes
                }
                id
            }
        }

    /** Devuelve false si el cliente tiene ventas registradas. */
    fun deleteClient(id: Int): Boolean =
        transaction {
            val n =
                SalesTable
                    .selectAll()
                    .where { SalesTable.clientId eq id }
                    .count()
            if (n > 0L) return@transaction false
            ClientsTable.deleteWhere { ClientsTable.id eq id }
            true
        }

    fun listSales(limit: Int = 500): List<SaleRecord> =
        transaction {
            (SalesTable innerJoin ClientsTable)
                .selectAll()
                .orderBy(SalesTable.soldAtEpochMs to SortOrder.DESC)
                .limit(limit)
                .map { it.toSaleRecord() }
        }

    fun loadAllSales(): List<SaleRecord> =
        transaction {
            (SalesTable innerJoin ClientsTable)
                .selectAll()
                .orderBy(SalesTable.soldAtEpochMs to SortOrder.DESC)
                .map { it.toSaleRecord() }
        }

    sealed class SaleRecordingResult {
        data class Ok(val saleId: Int) : SaleRecordingResult()

        data class Err(val message: String) : SaleRecordingResult()
    }

    fun recordSale(
        productId: Int,
        clientId: Int,
        quantitySold: Double,
        totalAmount: Double,
        soldAtEpochMs: Long,
        notes: String?,
        /** Porcentaje de ganancia usado solo para guardar la estimación (ej. 20 = 20 %). */
        marginPercentForEstimate: Double? = null,
    ): SaleRecordingResult =
        transaction {
            ClientsTable
                .selectAll()
                .where { ClientsTable.id eq clientId }
                .singleOrNull()
                ?: return@transaction SaleRecordingResult.Err("Cliente no encontrado.")

            val prow =
                ProductsTable
                    .selectAll()
                    .where { ProductsTable.id eq productId }
                    .singleOrNull()
                    ?: return@transaction SaleRecordingResult.Err("Lote no encontrado.")

            val stage = ProductStage.fromDb(prow[ProductsTable.stage])
            val isFailed = prow[ProductsTable.isFailed]
            val sellable = (stage == ProductStage.TERMINADO && !isFailed) || isFailed
            if (!sellable) {
                return@transaction SaleRecordingResult.Err(
                    "Sólo se venden postes en Terminado (OK) o lotes marcados como fallados.",
                )
            }
            val qtyAvail = prow[ProductsTable.quantity]
            if (quantitySold <= 0 || quantitySold - qtyAvail > 1e-6) {
                return@transaction SaleRecordingResult.Err(
                    "Cantidad inválida (disponible: ${fmt(qtyAvail)}).",
                )
            }
            if (totalAmount < 0) {
                return@transaction SaleRecordingResult.Err("El monto total no puede ser negativo.")
            }

            val provId = prow[ProductsTable.providerId]
            val provName =
                if (provId != null) {
                    PoleProvidersTable
                        .selectAll()
                        .where { PoleProvidersTable.id eq provId }
                        .singleOrNull()
                        ?.get(PoleProvidersTable.name)
                } else {
                    null
                }

            val unitPrice = if (quantitySold > 0) totalAmount / quantitySold else null

            val materialPer = prow[ProductsTable.acquisitionCostPerPole] ?: 0.0
            val transportTotalOnLot = transportCostSumForProduct(productId)
            val transportPerPole =
                if (qtyAvail > 1e-12) transportTotalOnLot / qtyAvail else 0.0
            val landedPerPole = materialPer + transportPerPole
            val procTotalOnLot =
                ProcessCostsTable
                    .selectAll()
                    .where { ProcessCostsTable.productId eq productId }
                    .sumOf { it[ProcessCostsTable.lineCost] }
            val procPerPole = if (qtyAvail > 1e-12) procTotalOnLot / qtyAvail else 0.0
            val materialPortion = quantitySold * materialPer
            val transportPortion = quantitySold * transportPerPole
            val acqPortion = materialPortion + transportPortion
            val procPortion = quantitySold * procPerPole
            val unitBasis = landedPerPole + procPerPole
            val suggestedTotal =
                marginPercentForEstimate?.let { m ->
                    quantitySold * unitBasis * (1.0 + m / 100.0)
                }

            val saleId =
                SalesTable.insert {
                    it[SalesTable.productId] = productId
                    it[SalesTable.clientId] = clientId
                    it[SalesTable.quantitySold] = quantitySold
                    it[SalesTable.totalAmount] = totalAmount
                    it[SalesTable.unitPrice] = unitPrice
                    it[SalesTable.soldAtEpochMs] = soldAtEpochMs
                    it[SalesTable.notes] = notes
                    it[SalesTable.snapshotProductName] = prow[ProductsTable.name]
                    it[SalesTable.snapshotProductLine] = prow[ProductsTable.productLine]
                    it[SalesTable.snapshotStage] = stage.name
                    it[SalesTable.snapshotWasFailed] = isFailed
                    it[SalesTable.snapshotProviderName] = provName
                    it[SalesTable.snapshotAcquisitionCostTotal] = acqPortion
                    it[SalesTable.snapshotAcquisitionMaterialTotal] = materialPortion
                    it[SalesTable.snapshotAcquisitionTransportTotal] = transportPortion
                    it[SalesTable.snapshotProcessingCostTotal] = procPortion
                    it[SalesTable.snapshotUnitCostBasis] = unitBasis
                    it[SalesTable.snapshotMarginPercent] = marginPercentForEstimate
                    it[SalesTable.snapshotSuggestedTotal] = suggestedTotal
                }[SalesTable.id]

            val remaining = qtyAvail - quantitySold
            if (remaining <= 1e-6) {
                ProductsTable.deleteWhere { ProductsTable.id eq productId }
            } else {
                ProductsTable.update({ ProductsTable.id eq productId }) {
                    it[ProductsTable.quantity] = remaining
                }
            }

            SaleRecordingResult.Ok(saleId)
        }

    fun salesAggregatedDaily(
        year: Int,
        month: Int,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<AccountingBucket> {
        val sales =
            loadAllSales().filter {
                val d = Instant.ofEpochMilli(it.soldAtEpochMs).atZone(zone).toLocalDate()
                d.year == year && d.monthValue == month
            }
        val fmtDay = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es"))
        return sales
            .groupBy { Instant.ofEpochMilli(it.soldAtEpochMs).atZone(zone).toLocalDate() }
            .toList()
            .sortedBy { it.first }
            .map { (d, list) ->
                AccountingBucket(
                    periodKey = d.toString(),
                    displayLabel = d.format(fmtDay),
                    totalAmount = list.sumOf { it.totalAmount },
                    totalPolesSold = list.sumOf { it.quantitySold },
                    saleCount = list.size,
                )
            }
    }

    fun salesAggregatedMonthly(
        year: Int,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<AccountingBucket> {
        val sales =
            loadAllSales().filter {
                val d = Instant.ofEpochMilli(it.soldAtEpochMs).atZone(zone).toLocalDate()
                d.year == year
            }
        val fmtMonth = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es"))
        return (1..12).map { m ->
            val ym = YearMonth.of(year, m)
            val list =
                sales.filter {
                    val d = Instant.ofEpochMilli(it.soldAtEpochMs).atZone(zone).toLocalDate()
                    d.year == year && d.monthValue == m
                }
            AccountingBucket(
                periodKey = ym.toString(),
                displayLabel = ym.atDay(1).format(fmtMonth).replaceFirstChar { it.titlecase(Locale("es")) },
                totalAmount = list.sumOf { it.totalAmount },
                totalPolesSold = list.sumOf { it.quantitySold },
                saleCount = list.size,
            )
        }
    }

    fun salesAggregatedYearly(zone: ZoneId = ZoneId.systemDefault()): List<AccountingBucket> {
        val sales = loadAllSales()
        val fmtYear = DateTimeFormatter.ofPattern("yyyy", Locale("es"))
        return sales
            .groupBy {
                Year.from(Instant.ofEpochMilli(it.soldAtEpochMs).atZone(zone).toLocalDate())
            }
            .toList()
            .sortedByDescending { it.first }
            .map { (y, list) ->
                val firstDay = LocalDate.of(y.value, 1, 1)
                AccountingBucket(
                    periodKey = y.toString(),
                    displayLabel = firstDay.format(fmtYear),
                    totalAmount = list.sumOf { it.totalAmount },
                    totalPolesSold = list.sumOf { it.quantitySold },
                    saleCount = list.size,
                )
            }
    }

    fun upsertProduct(
        id: Int?,
        name: String,
        productLine: String,
        stage: ProductStage,
        quantity: Double,
        notes: String?,
        catalogProductId: Int?,
        providerId: Int?,
        standardSalePrice: Double?,
        failedSalePrice: Double?,
        acquisitionCostPerPole: Double?,
        acquisitionStorageLocation: PoleStorageLocation = PoleStorageLocation.FABRICA,
    ): Int =
        transaction {
            val now = System.currentTimeMillis()
            if (id == null) {
                ProductsTable.insert {
                    it[ProductsTable.name] = name
                    it[ProductsTable.productLine] = productLine
                    it[ProductsTable.stage] = stage.name
                    it[ProductsTable.quantity] = quantity
                    it[ProductsTable.notes] = notes
                    it[ProductsTable.createdAtEpochMs] = now
                    it[ProductsTable.catalogProductId] = catalogProductId
                    it[ProductsTable.providerId] = providerId
                    it[ProductsTable.isFailed] = false
                    it[ProductsTable.failedAtStage] = null
                    it[ProductsTable.standardSalePrice] = standardSalePrice
                    it[ProductsTable.failedSalePrice] = failedSalePrice
                    it[ProductsTable.acquisitionCostPerPole] = acquisitionCostPerPole
                    it[ProductsTable.acquisitionStorageLocation] = acquisitionStorageLocation.name
                }[ProductsTable.id]
            } else {
                ProductsTable.update({ ProductsTable.id eq id }) {
                    it[ProductsTable.name] = name
                    it[ProductsTable.productLine] = productLine
                    it[ProductsTable.stage] = stage.name
                    it[ProductsTable.quantity] = quantity
                    it[ProductsTable.notes] = notes
                    it[ProductsTable.catalogProductId] = catalogProductId
                    it[ProductsTable.providerId] = providerId
                    it[ProductsTable.standardSalePrice] = standardSalePrice
                    it[ProductsTable.failedSalePrice] = failedSalePrice
                    it[ProductsTable.acquisitionCostPerPole] = acquisitionCostPerPole
                    it[ProductsTable.acquisitionStorageLocation] = acquisitionStorageLocation.name
                }
                id
            }
        }

    /**
     * Suma de todas las líneas de traslado del lote (costos cargados en predio + flete/grúa
     * prorrateados al cerrar viajes en «Traslados»). Se reparte entre la cantidad de postes
     * para el costo de adquisición puesto en planta.
     */
    fun acquisitionTransportTotalForProduct(productId: Int): Double =
        transaction {
            transportCostSumForProduct(productId)
        }

    fun listAcquisitionTransportForProduct(productId: Int): List<AcquisitionTransportLine> =
        transaction {
            AcquisitionTransportCostsTable
                .selectAll()
                .where { AcquisitionTransportCostsTable.productId eq productId }
                .orderBy(AcquisitionTransportCostsTable.id to SortOrder.ASC)
                .map { it.toAcquisitionTransportLine() }
        }

    /**
     * Reemplaza las líneas de traslado del lote. Si [location] es [PoleStorageLocation.FABRICA], sólo borra.
     */
    fun syncAcquisitionTransportCosts(
        productId: Int,
        location: PoleStorageLocation,
        lines: List<AcquisitionTransportLineDraft>,
    ) {
        transaction {
            AcquisitionTransportCostsTable.deleteWhere {
                AcquisitionTransportCostsTable.productId eq productId
            }
            if (location != PoleStorageLocation.EN_PROVEEDOR) return@transaction
            val now = System.currentTimeMillis()
            lines
                .map { d ->
                    val c = d.lineCost.coerceAtLeast(0.0)
                    val lbl = d.label.trim().ifBlank { "Traslado" }
                    Triple(lbl, c, d.notes)
                }
                .filter { (_, c, _) -> c > 1e-12 }
                .forEach { (lbl, c, n) ->
                    AcquisitionTransportCostsTable.insert {
                        it[AcquisitionTransportCostsTable.productId] = productId
                        it[AcquisitionTransportCostsTable.label] = lbl
                        it[AcquisitionTransportCostsTable.lineCost] = c
                        it[AcquisitionTransportCostsTable.notes] = n
                        it[AcquisitionTransportCostsTable.createdAtEpochMs] = now
                    }
                }
        }
    }

    fun deleteProduct(id: Int) {
        transaction {
            ProductsTable.deleteWhere { ProductsTable.id eq id }
        }
    }

    /**
     * Costos de insumo imputables al inventario actual (líneas con [ProcessCostsTable.productId] no nulo)
     * y totales históricos / valor de compra en stock.
     */
    fun accountingCostOverview(): AccountingCostOverview =
        transaction {
            val allTimeExpr = ProcessCostsTable.lineCost.sum()
            val allTime =
                ProcessCostsTable
                    .select(allTimeExpr)
                    .firstOrNull()
                    ?.getOrNull(allTimeExpr)
                    ?: 0.0
            val openExpr = ProcessCostsTable.lineCost.sum()
            val onOpen =
                ProcessCostsTable
                    .select(openExpr)
                    .where { ProcessCostsTable.productId.isNotNull() }
                    .firstOrNull()
                    ?.getOrNull(openExpr)
                    ?: 0.0
            val transportSumsByProductId =
                AcquisitionTransportCostsTable
                    .selectAll()
                    .where { AcquisitionTransportCostsTable.productId.isNotNull() }
                    .groupBy { it[AcquisitionTransportCostsTable.productId]!! }
                    .mapValues { (_, rows) ->
                        rows.sumOf { it[AcquisitionTransportCostsTable.lineCost] }
                    }
            val invAcq =
                ProductsTable
                    .selectAll()
                    .sumOf { r ->
                        val pid = r[ProductsTable.id]
                        val q = r[ProductsTable.quantity]
                        val t = transportSumsByProductId[pid] ?: 0.0
                        val land =
                            landedAcquisitionPerPole(
                                r[ProductsTable.acquisitionCostPerPole],
                                q,
                                t,
                            )
                        q * land
                    }
            val trAllExpr = AcquisitionTransportCostsTable.lineCost.sum()
            val trAllTime =
                AcquisitionTransportCostsTable
                    .select(trAllExpr)
                    .firstOrNull()
                    ?.getOrNull(trAllExpr)
                    ?: 0.0
            val trOpenExpr = AcquisitionTransportCostsTable.lineCost.sum()
            val trOnOpen =
                AcquisitionTransportCostsTable
                    .select(trOpenExpr)
                    .where { AcquisitionTransportCostsTable.productId.isNotNull() }
                    .firstOrNull()
                    ?.getOrNull(trOpenExpr)
                    ?: 0.0
            val soldAcqExpr = SalesTable.snapshotAcquisitionCostTotal.sum()
            val soldAcq =
                SalesTable
                    .select(soldAcqExpr)
                    .firstOrNull()
                    ?.getOrNull(soldAcqExpr)
                    ?: 0.0
            val soldProcExpr = SalesTable.snapshotProcessingCostTotal.sum()
            val soldProc =
                SalesTable
                    .select(soldProcExpr)
                    .firstOrNull()
                    ?.getOrNull(soldProcExpr)
                    ?: 0.0
            val soldTrExpr = SalesTable.snapshotAcquisitionTransportTotal.sum()
            val soldTr =
                SalesTable
                    .select(soldTrExpr)
                    .firstOrNull()
                    ?.getOrNull(soldTrExpr)
                    ?: 0.0
            AccountingCostOverview(
                totalProcessingCostAllTime = allTime,
                processingCostAttributedToOpenStock = onOpen,
                inventoryAcquisitionCostTotal = invAcq,
                soldAcquisitionCostTotal = soldAcq,
                soldProcessingCostTotal = soldProc,
                totalAcquisitionTransportAllTime = trAllTime,
                acquisitionTransportAttributedToOpenStock = trOnOpen,
                soldAcquisitionTransportTotal = soldTr,
            )
        }

    /** Costo de procesamiento (insumos) acumulado en un lote. */
    fun processingCostTotalForProduct(productId: Int): Double =
        transaction {
            ProcessCostsTable
                .selectAll()
                .where { ProcessCostsTable.productId eq productId }
                .sumOf { it[ProcessCostsTable.lineCost] }
        }

    /**
     * Vista previa para venta: el precio sugerido aplica [marginPercent] sobre el costo por poste
     * (materia prima + traslado total del lote prorrateado por poste + proceso con insumos).
     */
    fun saleCostPreview(
        productId: Int,
        quantitySold: Double,
        marginPercent: Double,
    ): SaleCostPreview? =
        transaction {
            val row =
                ProductsTable
                    .selectAll()
                    .where { ProductsTable.id eq productId }
                    .singleOrNull() ?: return@transaction null
            val qtyAvail = row[ProductsTable.quantity]
            if (quantitySold <= 1e-12 || qtyAvail <= 1e-12) return@transaction null
            val materialPer = row[ProductsTable.acquisitionCostPerPole]
            val transportTotal = transportCostSumForProduct(productId)
            val transportPerPole =
                if (qtyAvail > 1e-12) transportTotal / qtyAvail else 0.0
            val landedPerPole =
                landedAcquisitionPerPole(materialPer, qtyAvail, transportTotal)
            val procTotal =
                ProcessCostsTable
                    .selectAll()
                    .where { ProcessCostsTable.productId eq productId }
                    .sumOf { it[ProcessCostsTable.lineCost] }
            val procPerPole = procTotal / qtyAvail
            val unitBasis = landedPerPole + procPerPole
            val sugUnit = unitBasis * (1.0 + marginPercent / 100.0)
            val matPortion = quantitySold * (materialPer ?: 0.0)
            val trPortion = quantitySold * transportPerPole
            SaleCostPreview(
                quantityAvailable = qtyAvail,
                acquisitionMaterialPerPole = materialPer,
                acquisitionTransportPerPole = transportPerPole,
                landedAcquisitionPerPole = landedPerPole,
                processingCostTotalOnLot = procTotal,
                processingCostPerPole = procPerPole,
                unitCostBasis = unitBasis,
                acquisitionMaterialTotalForSaleQty = matPortion,
                acquisitionTransportTotalForSaleQty = trPortion,
                acquisitionTotalForSaleQty = matPortion + trPortion,
                processingTotalForSaleQty = quantitySold * procPerPole,
                suggestedUnitPrice = sugUnit,
                suggestedTotal = quantitySold * sugUnit,
            )
        }

    fun countByStage(): Map<ProductStage, Int> =
        transaction {
            val rows =
                ProductsTable
                    .selectAll()
                    .map { ProductStage.fromDb(it[ProductsTable.stage]) }
            ProductStage.entries.associateWith { s -> rows.count { it == s } }
        }

    /** Suma de cantidades de postes por etapa (incluye lotes fallados). */
    fun polesByStage(): Map<ProductStage, Double> =
        transaction {
            val rows =
                ProductsTable
                    .selectAll()
                    .map { ProductStage.fromDb(it[ProductsTable.stage]) to it[ProductsTable.quantity] }
            ProductStage.entries.associateWith { s ->
                rows.filter { it.first == s }.sumOf { it.second }
            }
        }

    fun failedProductCount(): Int =
        transaction {
            ProductsTable
                .selectAll()
                .where { ProductsTable.isFailed eq true }
                .count()
                .toInt()
        }

    /** Rough value of failed stock at clearance prices (qty × failed sale price when set). */
    fun failedStockSaleValueEstimate(): Double =
        transaction {
            ProductsTable
                .selectAll()
                .where {
                    ProductsTable.isFailed eq true and ProductsTable.failedSalePrice.isNotNull()
                }
                .sumOf { row ->
                    val q = row[ProductsTable.quantity]
                    val p = row[ProductsTable.failedSalePrice] ?: 0.0
                    q * p
                }
        }

    fun totalProcessCost(): Double =
        transaction {
            val sumExpr = ProcessCostsTable.lineCost.sum()
            ProcessCostsTable
                .select(sumExpr)
                .firstOrNull()
                ?.getOrNull(sumExpr)
                ?: 0.0
        }

    fun listResources(): List<Resource> =
        transaction {
            ResourcesTable
                .selectAll()
                .orderBy(ResourcesTable.name to SortOrder.ASC)
                .map {
                    Resource(
                        id = it[ResourcesTable.id],
                        name = it[ResourcesTable.name],
                        unit = it[ResourcesTable.unit],
                        costPerUnit = it[ResourcesTable.costPerUnit],
                    )
                }
        }

    fun upsertResource(
        id: Int?,
        name: String,
        unit: String,
        costPerUnit: Double,
    ): Int =
        transaction {
            if (id == null) {
                ResourcesTable.insert {
                    it[ResourcesTable.name] = name
                    it[ResourcesTable.unit] = unit
                    it[ResourcesTable.costPerUnit] = costPerUnit
                }[ResourcesTable.id]
            } else {
                ResourcesTable.update({ ResourcesTable.id eq id }) {
                    it[ResourcesTable.name] = name
                    it[ResourcesTable.unit] = unit
                    it[ResourcesTable.costPerUnit] = costPerUnit
                }
                id
            }
        }

    fun deleteResource(id: Int) {
        transaction {
            ResourcesTable.deleteWhere { ResourcesTable.id eq id }
        }
    }

    /** Partidas de inventario por insumo (cantidad, precio de compra, vencimiento). */
    fun listResourceStockLots(): List<ResourceStockLot> =
        transaction {
            (ResourceStockLotsTable innerJoin ResourcesTable)
                .selectAll()
                .map { it.toResourceStockLot() }
        }.sortedWith(
            compareBy<ResourceStockLot> { it.expirationDate ?: LocalDate.MAX }
                .thenByDescending { it.id },
        )

    fun upsertResourceStockLot(
        id: Int?,
        resourceId: Int,
        quantity: Double,
        acquisitionPricePerUnit: Double,
        expirationDate: LocalDate?,
        notes: String?,
    ): Int =
        transaction {
            val now = System.currentTimeMillis()
            val expStr = expirationDate?.toString()
            if (id == null) {
                ResourceStockLotsTable.insert {
                    it[ResourceStockLotsTable.resourceId] = resourceId
                    it[ResourceStockLotsTable.quantity] = quantity
                    it[ResourceStockLotsTable.acquisitionPricePerUnit] = acquisitionPricePerUnit
                    it[ResourceStockLotsTable.expirationDate] = expStr
                    it[ResourceStockLotsTable.acquiredAtEpochMs] = now
                    it[ResourceStockLotsTable.notes] = notes
                }[ResourceStockLotsTable.id]
            } else {
                ResourceStockLotsTable.update({ ResourceStockLotsTable.id eq id }) {
                    it[ResourceStockLotsTable.resourceId] = resourceId
                    it[ResourceStockLotsTable.quantity] = quantity
                    it[ResourceStockLotsTable.acquisitionPricePerUnit] = acquisitionPricePerUnit
                    it[ResourceStockLotsTable.expirationDate] = expStr
                    it[ResourceStockLotsTable.notes] = notes
                }
                id
            }
        }

    fun deleteResourceStockLot(id: Int) {
        transaction {
            ResourceStockLotsTable.deleteWhere { ResourceStockLotsTable.id eq id }
        }
    }

    /** Valor estimado del inventario de insumos (cantidad × precio de compra por partida). */
    fun resourceStockTotalValueEstimate(): Double =
        transaction {
            (ResourceStockLotsTable innerJoin ResourcesTable)
                .selectAll()
                .sumOf { row ->
                    row[ResourceStockLotsTable.quantity] * row[ResourceStockLotsTable.acquisitionPricePerUnit]
                }
        }

    fun listStageResourceTemplates(fromStage: ProductStage): List<StageResourceTemplate> =
        transaction {
            (StageResourceTemplatesTable innerJoin ResourcesTable)
                .selectAll()
                .where { StageResourceTemplatesTable.fromStage eq fromStage.name }
                .orderBy(StageResourceTemplatesTable.displayOrder to SortOrder.ASC)
                .orderBy(ResourcesTable.name to SortOrder.ASC)
                .map {
                    StageResourceTemplate(
                        id = it[StageResourceTemplatesTable.id],
                        fromStage = ProductStage.fromDb(it[StageResourceTemplatesTable.fromStage]),
                        resourceId = it[StageResourceTemplatesTable.resourceId],
                        resourceName = it[ResourcesTable.name],
                        resourceUnit = it[ResourcesTable.unit],
                        amountPerPole = it[StageResourceTemplatesTable.amountPerPole],
                        notes = it[StageResourceTemplatesTable.notes],
                        displayOrder = it[StageResourceTemplatesTable.displayOrder],
                    )
                }
        }

    /**
     * Cantidades sugeridas para una transformación: receta × número de postes.
     * El operador puede ajustarlas en el diálogo antes de registrar.
     */
    fun suggestResourceUsesFromRecipe(
        fromStage: ProductStage,
        poleCount: Double,
    ): List<ResourceUse> {
        if (poleCount <= 0) return emptyList()
        return listStageResourceTemplates(fromStage).map { t ->
            ResourceUse(
                resourceId = t.resourceId,
                amount = t.amountPerPole * poleCount,
                label = t.notes.orEmpty(),
            )
        }
    }

    fun upsertStageResourceTemplate(
        id: Int?,
        fromStage: ProductStage,
        resourceId: Int,
        amountPerPole: Double,
        notes: String?,
        displayOrder: Int,
    ): Int =
        transaction {
            if (id != null) {
                StageResourceTemplatesTable.update({ StageResourceTemplatesTable.id eq id }) {
                    it[StageResourceTemplatesTable.fromStage] = fromStage.name
                    it[StageResourceTemplatesTable.resourceId] = resourceId
                    it[StageResourceTemplatesTable.amountPerPole] = amountPerPole
                    it[StageResourceTemplatesTable.notes] = notes
                    it[StageResourceTemplatesTable.displayOrder] = displayOrder
                }
                return@transaction id
            }
            val existing =
                StageResourceTemplatesTable
                    .selectAll()
                    .where {
                        (StageResourceTemplatesTable.fromStage eq fromStage.name) and
                            (StageResourceTemplatesTable.resourceId eq resourceId)
                    }
                    .firstOrNull()
            if (existing != null) {
                val eid = existing[StageResourceTemplatesTable.id]
                StageResourceTemplatesTable.update({ StageResourceTemplatesTable.id eq eid }) {
                    it[StageResourceTemplatesTable.amountPerPole] = amountPerPole
                    it[StageResourceTemplatesTable.notes] = notes
                    it[StageResourceTemplatesTable.displayOrder] = displayOrder
                }
                return@transaction eid
            }
            StageResourceTemplatesTable.insert {
                it[StageResourceTemplatesTable.fromStage] = fromStage.name
                it[StageResourceTemplatesTable.resourceId] = resourceId
                it[StageResourceTemplatesTable.amountPerPole] = amountPerPole
                it[StageResourceTemplatesTable.notes] = notes
                it[StageResourceTemplatesTable.displayOrder] = displayOrder
            }[StageResourceTemplatesTable.id]
        }

    fun deleteStageResourceTemplate(templateId: Int) {
        transaction {
            StageResourceTemplatesTable.deleteWhere { StageResourceTemplatesTable.id eq templateId }
        }
    }

    fun listCostsForProduct(productId: Int): List<ProcessCostLine> =
        transaction {
            (ProcessCostsTable leftJoin ResourcesTable)
                .selectAll()
                .where { ProcessCostsTable.productId eq productId }
                .orderBy(ProcessCostsTable.createdAtEpochMs to SortOrder.DESC)
                .map { it.toProcessCostLine() }
        }

    fun listCostsForTransformation(transformationId: Int): List<ProcessCostLine> =
        transaction {
            (ProcessCostsTable leftJoin ResourcesTable)
                .selectAll()
                .where { ProcessCostsTable.transformationId eq transformationId }
                .orderBy(ProcessCostsTable.createdAtEpochMs to SortOrder.ASC)
                .map { it.toProcessCostLine() }
        }

    data class ResourceUse(
        val resourceId: Int,
        val amount: Double,
        val label: String,
    )

    data class SourceDraft(
        val sourceProductId: Int,
        val quantity: Double,
    )

    private fun fmt(v: Double): String =
        if (v % 1.0 == 0.0) v.toInt().toString() else "%.2f".format(v)

    /** Resuelve y valida lotes fuente; si [Pair.second] no es null, hubo error. */
    private fun org.jetbrains.exposed.sql.Transaction.sourceLotsOrErr(
        fromStage: ProductStage,
        inputs: List<SourceDraft>,
    ): Pair<List<Pair<ResultRow, Double>>, String?> {
        if (inputs.isEmpty()) return Pair(emptyList(), "Seleccione al menos un lote origen.")
        if (inputs.any { it.quantity <= 0 }) {
            return Pair(emptyList(), "Las cantidades deben ser mayores a cero.")
        }
        val mergedByBatch: Map<Int, Double> =
            inputs.groupBy { it.sourceProductId }
                .mapValues { (_, list) -> list.sumOf { it.quantity } }
        val sources = mutableListOf<Pair<ResultRow, Double>>()
        for ((pid, qty) in mergedByBatch) {
            val row =
                ProductsTable
                    .selectAll()
                    .where { ProductsTable.id eq pid }
                    .singleOrNull()
                    ?: return Pair(emptyList(), "Lote origen $pid no existe.")
            val stage = ProductStage.fromDb(row[ProductsTable.stage])
            if (stage != fromStage) {
                return Pair(
                    emptyList(),
                    "El lote '${row[ProductsTable.name]}' no está en ${fromStage.shortCode}.",
                )
            }
            if (row[ProductsTable.isFailed]) {
                return Pair(
                    emptyList(),
                    "El lote '${row[ProductsTable.name]}' está marcado como fallado.",
                )
            }
            val storageLoc =
                PoleStorageLocation.fromDb(row[ProductsTable.acquisitionStorageLocation])
            if (storageLoc != PoleStorageLocation.FABRICA) {
                return Pair(
                    emptyList(),
                    "El lote '${row[ProductsTable.name]}' aún no está en fábrica (${storageLoc.shortLabel}). " +
                        "Registre la llegada en «Traslados» antes de procesar.",
                )
            }
            val available = row[ProductsTable.quantity]
            if (qty - available > 1e-6) {
                return Pair(
                    emptyList(),
                    "El lote '${row[ProductsTable.name]}' sólo tiene ${fmt(available)} " +
                        "disponibles (pidió ${fmt(qty)}).",
                )
            }
            sources.add(row to qty)
        }
        return Pair(sources, null)
    }

    sealed class TransformationResult {
        data class Ok(val id: Int) : TransformationResult()

        data class Err(val message: String) : TransformationResult()
    }

    /**
     * Crea una transformación que toma poles de [inputs] (lotes en [fromStage]),
     * produce un lote exitoso de tamaño [successCount] en [fromStage].next() y,
     * si [failedCount] > 0, un lote fallado de ese tamaño en [fromStage].
     *
     * Se valida que la suma de cantidades de entrada sea igual a
     * successCount + failedCount, y que cada lote origen tenga stock suficiente
     * y no esté marcado como fallado.
     */
    fun createTransformation(
        fromStage: ProductStage,
        inputs: List<SourceDraft>,
        successCount: Double,
        failedCount: Double,
        durationMinutes: Int,
        processedAtEpochMs: Long,
        notes: String?,
        resourceUses: List<ResourceUse>,
    ): TransformationResult =
        transaction {
            val toStage =
                fromStage.next()
                    ?: return@transaction TransformationResult.Err(
                        "La etapa ${fromStage.shortCode} no tiene siguiente fase.",
                    )
            if (inputs.isEmpty()) {
                return@transaction TransformationResult.Err("Seleccione al menos un lote origen.")
            }
            if (inputs.any { it.quantity <= 0 }) {
                return@transaction TransformationResult.Err("Las cantidades deben ser mayores a cero.")
            }
            if (successCount < 0 || failedCount < 0) {
                return@transaction TransformationResult.Err("Los conteos no pueden ser negativos.")
            }
            val totalInput = inputs.sumOf { it.quantity }
            val totalOutput = successCount + failedCount
            if (kotlin.math.abs(totalInput - totalOutput) > 1e-6) {
                return@transaction TransformationResult.Err(
                    "Éxitos + fallados (${fmt(totalOutput)}) debe ser igual " +
                        "al total tomado de los lotes (${fmt(totalInput)}).",
                )
            }

            val (sources, srcErr) = sourceLotsOrErr(fromStage, inputs)
            if (srcErr != null) return@transaction TransformationResult.Err(srcErr)

            val now = System.currentTimeMillis()
            val transformationId =
                TransformationsTable.insert {
                    it[TransformationsTable.fromStage] = fromStage.name
                    it[TransformationsTable.toStage] = toStage.name
                    it[TransformationsTable.processingStatus] =
                        TransformationProcessingStatus.COMPLETED.name
                    it[TransformationsTable.startedAtEpochMs] = processedAtEpochMs
                    it[TransformationsTable.processedAtEpochMs] = processedAtEpochMs
                    it[TransformationsTable.durationMinutes] = durationMinutes
                    it[TransformationsTable.successCount] = successCount
                    it[TransformationsTable.failedCount] = failedCount
                    it[TransformationsTable.notes] = notes
                    it[TransformationsTable.createdAtEpochMs] = now
                }[TransformationsTable.id]

            val first = sources.first().first
            val inheritedName = first[ProductsTable.name]
            val inheritedLine = first[ProductsTable.productLine]
            val inheritedCatalog = first[ProductsTable.catalogProductId]
            val inheritedProvider = first[ProductsTable.providerId]
            val inheritedStandardPrice = first[ProductsTable.standardSalePrice]
            val inheritedFailedPrice = first[ProductsTable.failedSalePrice]
            val acqNumerator =
                sources.sumOf { (row, takeQty) ->
                    val sid = row[ProductsTable.id]
                    val tSum = transportCostSumForProduct(sid)
                    val qLot = row[ProductsTable.quantity]
                    val landed =
                        landedAcquisitionPerPole(
                            row[ProductsTable.acquisitionCostPerPole],
                            qLot,
                            tSum,
                        )
                    takeQty * landed
                }
            val blendedAcq: Double? =
                if (totalInput > 1e-12 && acqNumerator > 1e-12) acqNumerator / totalInput else null

            sources.forEach { (row, takeQty) ->
                val sourceId = row[ProductsTable.id]
                TransformationInputsTable.insert {
                    it[TransformationInputsTable.transformationId] = transformationId
                    it[TransformationInputsTable.sourceProductId] = sourceId
                    it[TransformationInputsTable.sourceName] = row[ProductsTable.name]
                    it[TransformationInputsTable.sourceLine] = row[ProductsTable.productLine]
                    it[TransformationInputsTable.quantity] = takeQty
                }
                val remaining = row[ProductsTable.quantity] - takeQty
                if (remaining <= 1e-6) {
                    ProductsTable.deleteWhere { ProductsTable.id eq sourceId }
                } else {
                    ProductsTable.update({ ProductsTable.id eq sourceId }) {
                        it[ProductsTable.quantity] = remaining
                    }
                }
            }

            var successProductId: Int? = null
            var failedProductId: Int? = null

            if (successCount > 0.0) {
                successProductId =
                    ProductsTable.insert {
                        it[ProductsTable.name] = inheritedName
                        it[ProductsTable.productLine] = inheritedLine
                        it[ProductsTable.stage] = toStage.name
                        it[ProductsTable.quantity] = successCount
                        it[ProductsTable.notes] =
                            "Producto de la transformación #$transformationId " +
                                "(${fromStage.shortCode} → ${toStage.shortCode})"
                        it[ProductsTable.createdAtEpochMs] = now
                        it[ProductsTable.catalogProductId] = inheritedCatalog
                        it[ProductsTable.providerId] = inheritedProvider
                        it[ProductsTable.isFailed] = false
                        it[ProductsTable.failedAtStage] = null
                        it[ProductsTable.standardSalePrice] = inheritedStandardPrice
                        it[ProductsTable.failedSalePrice] = inheritedFailedPrice
                        it[ProductsTable.acquisitionCostPerPole] = blendedAcq
                        it[ProductsTable.acquisitionStorageLocation] = PoleStorageLocation.FABRICA.name
                    }[ProductsTable.id]
            }

            if (failedCount > 0.0) {
                failedProductId =
                    ProductsTable.insert {
                        it[ProductsTable.name] = inheritedName
                        it[ProductsTable.productLine] = inheritedLine
                        it[ProductsTable.stage] = fromStage.name
                        it[ProductsTable.quantity] = failedCount
                        it[ProductsTable.notes] =
                            "Fallado durante la transformación #$transformationId en ${fromStage.shortCode}"
                        it[ProductsTable.createdAtEpochMs] = now
                        it[ProductsTable.catalogProductId] = inheritedCatalog
                        it[ProductsTable.providerId] = inheritedProvider
                        it[ProductsTable.isFailed] = true
                        it[ProductsTable.failedAtStage] = fromStage.name
                        it[ProductsTable.standardSalePrice] = inheritedStandardPrice
                        it[ProductsTable.failedSalePrice] = inheritedFailedPrice
                        it[ProductsTable.acquisitionCostPerPole] = blendedAcq
                        it[ProductsTable.acquisitionStorageLocation] = PoleStorageLocation.FABRICA.name
                    }[ProductsTable.id]
            }

            // Costo del proceso se asocia al lote resultante (prefiere el exitoso).
            val costProductId =
                successProductId
                    ?: failedProductId
                    ?: return@transaction TransformationResult.Err(
                        "La transformación no produjo ningún lote (éxitos y fallados son cero).",
                    )

            val resourcesById =
                ResourcesTable
                    .selectAll()
                    .associateBy { it[ResourcesTable.id] }

            resourceUses.forEach { u ->
                val r = resourcesById[u.resourceId] ?: return@forEach
                val cpu = r[ResourcesTable.costPerUnit]
                ProcessCostsTable.insert {
                    it[ProcessCostsTable.productId] = costProductId
                    it[ProcessCostsTable.transformationId] = transformationId
                    it[ProcessCostsTable.fromStage] = fromStage.name
                    it[ProcessCostsTable.toStage] = toStage.name
                    it[ProcessCostsTable.resourceId] = u.resourceId
                    it[ProcessCostsTable.amountUsed] = u.amount
                    it[ProcessCostsTable.lineCost] = u.amount * cpu
                    it[ProcessCostsTable.label] = u.label
                    it[ProcessCostsTable.createdAtEpochMs] = now
                }
            }

            TransformationResult.Ok(transformationId)
        }

    private fun org.jetbrains.exposed.sql.Transaction.hydrateTransformationRows(
        trows: List<ResultRow>,
    ): List<Transformation> {
        if (trows.isEmpty()) return emptyList()
        val ids = trows.map { it[TransformationsTable.id] }
        val inputsByTx =
            TransformationInputsTable
                .selectAll()
                .where { TransformationInputsTable.transformationId inList ids }
                .groupBy { it[TransformationInputsTable.transformationId] }
                .mapValues { (_, rows) ->
                    rows.map {
                        TransformationInputView(
                            id = it[TransformationInputsTable.id],
                            sourceProductId = it[TransformationInputsTable.sourceProductId],
                            sourceName = it[TransformationInputsTable.sourceName],
                            sourceLine = it[TransformationInputsTable.sourceLine],
                            quantity = it[TransformationInputsTable.quantity],
                        )
                    }
                }

        val costByTx =
            ProcessCostsTable
                .selectAll()
                .where { ProcessCostsTable.transformationId inList ids }
                .groupBy { it[ProcessCostsTable.transformationId]!! }
                .mapValues { (_, rows) -> rows.sumOf { it[ProcessCostsTable.lineCost] } }

        return trows.map { row ->
            val id = row[TransformationsTable.id]
            val st =
                TransformationProcessingStatus.fromDb(
                    row[TransformationsTable.processingStatus],
                )
            Transformation(
                id = id,
                fromStage = ProductStage.fromDb(row[TransformationsTable.fromStage]),
                toStage = ProductStage.fromDb(row[TransformationsTable.toStage]),
                processingStatus = st,
                startedAtEpochMs = row[TransformationsTable.startedAtEpochMs],
                processedAtEpochMs = row[TransformationsTable.processedAtEpochMs],
                durationMinutes = row[TransformationsTable.durationMinutes],
                successCount = row[TransformationsTable.successCount],
                failedCount = row[TransformationsTable.failedCount],
                notes = row[TransformationsTable.notes],
                createdAtEpochMs = row[TransformationsTable.createdAtEpochMs],
                inputs = inputsByTx[id].orEmpty(),
                totalCost = costByTx[id] ?: 0.0,
            )
        }
    }

    /**
     * Declara que comenzó un proceso hacia la siguiente etapa. El inventario no cambia hasta
     * [completeStageProcess] o se elimina el registro con [cancelStageProcess].
     */
    fun startStageProcess(
        fromStage: ProductStage,
        inputs: List<SourceDraft>,
        startedAtEpochMs: Long,
        notes: String?,
    ): TransformationResult =
        transaction {
            val toStage =
                fromStage.next()
                    ?: return@transaction TransformationResult.Err(
                        "La etapa ${fromStage.shortCode} no tiene siguiente fase.",
                    )
            val (sources, srcErr) = sourceLotsOrErr(fromStage, inputs)
            if (srcErr != null) return@transaction TransformationResult.Err(srcErr)

            val now = System.currentTimeMillis()
            val transformationId =
                TransformationsTable.insert {
                    it[TransformationsTable.fromStage] = fromStage.name
                    it[TransformationsTable.toStage] = toStage.name
                    it[TransformationsTable.processingStatus] =
                        TransformationProcessingStatus.IN_PROGRESS.name
                    it[TransformationsTable.startedAtEpochMs] = startedAtEpochMs
                    it[TransformationsTable.processedAtEpochMs] = startedAtEpochMs
                    it[TransformationsTable.durationMinutes] = 0
                    it[TransformationsTable.successCount] = 0.0
                    it[TransformationsTable.failedCount] = 0.0
                    it[TransformationsTable.notes] = notes
                    it[TransformationsTable.createdAtEpochMs] = now
                }[TransformationsTable.id]

            sources.forEach { (row, takeQty) ->
                val sourceId = row[ProductsTable.id]
                TransformationInputsTable.insert {
                    it[TransformationInputsTable.transformationId] = transformationId
                    it[TransformationInputsTable.sourceProductId] = sourceId
                    it[TransformationInputsTable.sourceName] = row[ProductsTable.name]
                    it[TransformationInputsTable.sourceLine] = row[ProductsTable.productLine]
                    it[TransformationInputsTable.quantity] = takeQty
                }
            }

            TransformationResult.Ok(transformationId)
        }

    /** Cierra un proceso en curso aplicando inventario e insumos. */
    fun completeStageProcess(
        transformationId: Int,
        successCount: Double,
        failedCount: Double,
        durationMinutes: Int,
        processedAtEpochMs: Long,
        completeNotes: String?,
        resourceUses: List<ResourceUse>,
    ): TransformationResult =
        transaction {
            val tRow =
                TransformationsTable
                    .selectAll()
                    .where { TransformationsTable.id eq transformationId }
                    .singleOrNull()
                    ?: return@transaction TransformationResult.Err("Transformación no encontrada.")
            if (
                TransformationProcessingStatus.fromDb(tRow[TransformationsTable.processingStatus]) !=
                TransformationProcessingStatus.IN_PROGRESS
            ) {
                return@transaction TransformationResult.Err(
                    "Sólo se puede finalizar un proceso que esté en curso.",
                )
            }

            val fromStage = ProductStage.fromDb(tRow[TransformationsTable.fromStage])
            val toStage = ProductStage.fromDb(tRow[TransformationsTable.toStage])

            if (successCount < 0 || failedCount < 0) {
                return@transaction TransformationResult.Err("Los conteos no pueden ser negativos.")
            }

            val inputRows =
                TransformationInputsTable
                    .selectAll()
                    .where { TransformationInputsTable.transformationId eq transformationId }
                    .toList()
            if (inputRows.isEmpty()) {
                return@transaction TransformationResult.Err("No hay entradas planeadas para esta transformación.")
            }

            val drafts =
                inputRows.mapNotNull { r ->
                    val pid = r[TransformationInputsTable.sourceProductId] ?: return@mapNotNull null
                    SourceDraft(pid, r[TransformationInputsTable.quantity])
                }
            if (drafts.size != inputRows.size) {
                return@transaction TransformationResult.Err(
                    "Faltan referencias a lotes origen; no se puede completar.",
                )
            }

            val totalInput = drafts.sumOf { it.quantity }
            val totalOutput = successCount + failedCount
            if (kotlin.math.abs(totalInput - totalOutput) > 1e-6) {
                return@transaction TransformationResult.Err(
                    "Éxitos + fallados (${fmt(totalOutput)}) debe ser igual " +
                        "al total planeado (${fmt(totalInput)}).",
                )
            }

            val (sources, srcErr) = sourceLotsOrErr(fromStage, drafts)
            if (srcErr != null) return@transaction TransformationResult.Err(srcErr)

            val now = System.currentTimeMillis()
            val startNotes = tRow[TransformationsTable.notes]
            val mergedNotes =
                listOfNotNull(
                    startNotes?.trim()?.takeIf { it.isNotEmpty() },
                    completeNotes?.trim()?.takeIf { it.isNotEmpty() },
                ).joinToString("\n---\n")
                    .ifBlank { null }

            val first = sources.first().first
            val inheritedName = first[ProductsTable.name]
            val inheritedLine = first[ProductsTable.productLine]
            val inheritedCatalog = first[ProductsTable.catalogProductId]
            val inheritedProvider = first[ProductsTable.providerId]
            val inheritedStandardPrice = first[ProductsTable.standardSalePrice]
            val inheritedFailedPrice = first[ProductsTable.failedSalePrice]
            val acqNumerator =
                sources.sumOf { (row, takeQty) ->
                    val sid = row[ProductsTable.id]
                    val tSum = transportCostSumForProduct(sid)
                    val qLot = row[ProductsTable.quantity]
                    val landed =
                        landedAcquisitionPerPole(
                            row[ProductsTable.acquisitionCostPerPole],
                            qLot,
                            tSum,
                        )
                    takeQty * landed
                }
            val blendedAcq: Double? =
                if (totalInput > 1e-12 && acqNumerator > 1e-12) acqNumerator / totalInput else null

            sources.forEach { (row, takeQty) ->
                val sourceId = row[ProductsTable.id]
                val remaining = row[ProductsTable.quantity] - takeQty
                if (remaining <= 1e-6) {
                    ProductsTable.deleteWhere { ProductsTable.id eq sourceId }
                } else {
                    ProductsTable.update({ ProductsTable.id eq sourceId }) {
                        it[ProductsTable.quantity] = remaining
                    }
                }
            }

            var successProductId: Int? = null
            var failedProductId: Int? = null

            if (successCount > 0.0) {
                successProductId =
                    ProductsTable.insert {
                        it[ProductsTable.name] = inheritedName
                        it[ProductsTable.productLine] = inheritedLine
                        it[ProductsTable.stage] = toStage.name
                        it[ProductsTable.quantity] = successCount
                        it[ProductsTable.notes] =
                            "Producto de la transformación #$transformationId " +
                                "(${fromStage.shortCode} → ${toStage.shortCode})"
                        it[ProductsTable.createdAtEpochMs] = now
                        it[ProductsTable.catalogProductId] = inheritedCatalog
                        it[ProductsTable.providerId] = inheritedProvider
                        it[ProductsTable.isFailed] = false
                        it[ProductsTable.failedAtStage] = null
                        it[ProductsTable.standardSalePrice] = inheritedStandardPrice
                        it[ProductsTable.failedSalePrice] = inheritedFailedPrice
                        it[ProductsTable.acquisitionCostPerPole] = blendedAcq
                        it[ProductsTable.acquisitionStorageLocation] = PoleStorageLocation.FABRICA.name
                    }[ProductsTable.id]
            }

            if (failedCount > 0.0) {
                failedProductId =
                    ProductsTable.insert {
                        it[ProductsTable.name] = inheritedName
                        it[ProductsTable.productLine] = inheritedLine
                        it[ProductsTable.stage] = fromStage.name
                        it[ProductsTable.quantity] = failedCount
                        it[ProductsTable.notes] =
                            "Fallado durante la transformación #$transformationId en ${fromStage.shortCode}"
                        it[ProductsTable.createdAtEpochMs] = now
                        it[ProductsTable.catalogProductId] = inheritedCatalog
                        it[ProductsTable.providerId] = inheritedProvider
                        it[ProductsTable.isFailed] = true
                        it[ProductsTable.failedAtStage] = fromStage.name
                        it[ProductsTable.standardSalePrice] = inheritedStandardPrice
                        it[ProductsTable.failedSalePrice] = inheritedFailedPrice
                        it[ProductsTable.acquisitionCostPerPole] = blendedAcq
                        it[ProductsTable.acquisitionStorageLocation] = PoleStorageLocation.FABRICA.name
                    }[ProductsTable.id]
            }

            val costProductId =
                successProductId
                    ?: failedProductId
                    ?: return@transaction TransformationResult.Err(
                        "La transformación no produjo ningún lote (éxitos y fallados son cero).",
                    )

            val resourcesById =
                ResourcesTable
                    .selectAll()
                    .associateBy { it[ResourcesTable.id] }

            resourceUses.forEach { u ->
                val r = resourcesById[u.resourceId] ?: return@forEach
                val cpu = r[ResourcesTable.costPerUnit]
                ProcessCostsTable.insert {
                    it[ProcessCostsTable.productId] = costProductId
                    it[ProcessCostsTable.transformationId] = transformationId
                    it[ProcessCostsTable.fromStage] = fromStage.name
                    it[ProcessCostsTable.toStage] = toStage.name
                    it[ProcessCostsTable.resourceId] = u.resourceId
                    it[ProcessCostsTable.amountUsed] = u.amount
                    it[ProcessCostsTable.lineCost] = u.amount * cpu
                    it[ProcessCostsTable.label] = u.label
                    it[ProcessCostsTable.createdAtEpochMs] = now
                }
            }

            TransformationsTable.update({ TransformationsTable.id eq transformationId }) {
                it[TransformationsTable.processingStatus] =
                    TransformationProcessingStatus.COMPLETED.name
                it[TransformationsTable.successCount] = successCount
                it[TransformationsTable.failedCount] = failedCount
                it[TransformationsTable.processedAtEpochMs] = processedAtEpochMs
                it[TransformationsTable.durationMinutes] = durationMinutes
                it[TransformationsTable.notes] = mergedNotes
            }

            TransformationResult.Ok(transformationId)
        }

    /** Descarta un proceso iniciado sin mover inventario. */
    fun cancelStageProcess(transformationId: Int): TransformationResult =
        transaction {
            val tRow =
                TransformationsTable
                    .selectAll()
                    .where { TransformationsTable.id eq transformationId }
                    .singleOrNull()
                    ?: return@transaction TransformationResult.Err("Transformación no encontrada.")
            if (
                TransformationProcessingStatus.fromDb(tRow[TransformationsTable.processingStatus]) !=
                TransformationProcessingStatus.IN_PROGRESS
            ) {
                return@transaction TransformationResult.Err("Sólo se cancelan procesos en curso.")
            }
            TransformationsTable.deleteWhere { TransformationsTable.id eq transformationId }
            TransformationResult.Ok(transformationId)
        }

    fun getTransformation(id: Int): Transformation? =
        transaction {
            val row =
                TransformationsTable
                    .selectAll()
                    .where { TransformationsTable.id eq id }
                    .singleOrNull() ?: return@transaction null
            hydrateTransformationRows(listOf(row)).firstOrNull()
        }

    fun listInProgressTransformations(fromStage: ProductStage): List<Transformation> =
        transaction {
            val trows =
                TransformationsTable
                    .selectAll()
                    .where {
                        (TransformationsTable.processingStatus eq
                            TransformationProcessingStatus.IN_PROGRESS.name) and
                            (TransformationsTable.fromStage eq fromStage.name)
                    }
                    .orderBy(
                        TransformationsTable.startedAtEpochMs to SortOrder.DESC_NULLS_LAST,
                    )
                    .toList()
            hydrateTransformationRows(trows)
        }

    fun listTransformations(limit: Int = 200): List<Transformation> =
        transaction {
            val trows =
                TransformationsTable
                    .selectAll()
                    .orderBy(TransformationsTable.processedAtEpochMs to SortOrder.DESC)
                    .limit(limit)
                    .toList()
            if (trows.isEmpty()) return@transaction emptyList()
            hydrateTransformationRows(trows)
        }

    /** Etapas donde se puede registrar una falla (antes de TERMINADO). */
    private val failureStages = setOf(ProductStage.CRUDO, ProductStage.DESCORTEZADO, ProductStage.TRATADO)

    fun markProductFailed(
        productId: Int,
        failedAtStage: ProductStage,
        failedSalePrice: Double,
        appendToNotes: String?,
    ): Boolean =
        transaction {
            if (failedAtStage !in failureStages) return@transaction false
            val row =
                ProductsTable
                    .selectAll()
                    .where { ProductsTable.id eq productId }
                    .singleOrNull() ?: return@transaction false
            val current = ProductStage.fromDb(row[ProductsTable.stage])
            if (current == ProductStage.TERMINADO) return@transaction false
            val note = row[ProductsTable.notes]
            val extra =
                buildString {
                    appendToNotes?.trim()?.takeIf { it.isNotEmpty() }?.let {
                        append(it)
                    }
                }
            val merged =
                when {
                    extra.isEmpty() -> note
                    note.isNullOrBlank() -> extra
                    else -> "$note\n$extra"
                }
            ProductsTable.update({ ProductsTable.id eq productId }) {
                it[ProductsTable.isFailed] = true
                it[ProductsTable.failedAtStage] = failedAtStage.name
                it[ProductsTable.failedSalePrice] = failedSalePrice
                it[ProductsTable.notes] = merged
            }
            true
        }

    fun clearProductFailure(productId: Int): Boolean =
        transaction {
            val n =
                ProductsTable.update({ ProductsTable.id eq productId }) {
                    it[ProductsTable.isFailed] = false
                    it[ProductsTable.failedAtStage] = null
                    it[ProductsTable.failedSalePrice] = null
                }
            n > 0
        }

    private fun ResultRow.toResourceStockLot(): ResourceStockLot {
        val expRaw = this[ResourceStockLotsTable.expirationDate]
        val exp =
            try {
                expRaw?.let { LocalDate.parse(it) }
            } catch (_: Exception) {
                null
            }
        return ResourceStockLot(
            id = this[ResourceStockLotsTable.id],
            resourceId = this[ResourceStockLotsTable.resourceId],
            resourceName = this[ResourcesTable.name],
            resourceUnit = this[ResourcesTable.unit],
            quantity = this[ResourceStockLotsTable.quantity],
            acquisitionPricePerUnit = this[ResourceStockLotsTable.acquisitionPricePerUnit],
            expirationDate = exp,
            acquiredAtEpochMs = this[ResourceStockLotsTable.acquiredAtEpochMs],
            notes = this[ResourceStockLotsTable.notes],
        )
    }

    private fun ResultRow.toProcessCostLine(): ProcessCostLine =
        ProcessCostLine(
            id = this[ProcessCostsTable.id],
            productId = this[ProcessCostsTable.productId],
            transformationId = this[ProcessCostsTable.transformationId],
            fromStage = ProductStage.fromDb(this[ProcessCostsTable.fromStage]),
            toStage = ProductStage.fromDb(this[ProcessCostsTable.toStage]),
            resourceId = this[ProcessCostsTable.resourceId],
            resourceName = this.getOrNull(ResourcesTable.name),
            amountUsed = this[ProcessCostsTable.amountUsed],
            lineCost = this[ProcessCostsTable.lineCost],
            label = this[ProcessCostsTable.label],
            createdAtEpochMs = this[ProcessCostsTable.createdAtEpochMs],
        )

    private fun ResultRow.toProduct(): Product {
        val failedAtRaw = this[ProductsTable.failedAtStage]
        return Product(
            id = this[ProductsTable.id],
            name = this[ProductsTable.name],
            productLine = this[ProductsTable.productLine],
            stage = ProductStage.fromDb(this[ProductsTable.stage]),
            quantity = this[ProductsTable.quantity],
            notes = this[ProductsTable.notes],
            createdAtEpochMs = this[ProductsTable.createdAtEpochMs],
            catalogProductId = this[ProductsTable.catalogProductId],
            providerId = this[ProductsTable.providerId],
            providerName = this.getOrNull(PoleProvidersTable.name),
            isFailed = this[ProductsTable.isFailed],
            failedAtStage = failedAtRaw?.let { ProductStage.fromDb(it) },
            standardSalePrice = this[ProductsTable.standardSalePrice],
            failedSalePrice = this[ProductsTable.failedSalePrice],
            acquisitionCostPerPole = this[ProductsTable.acquisitionCostPerPole],
            acquisitionStorageLocation =
                PoleStorageLocation.fromDb(this[ProductsTable.acquisitionStorageLocation]),
        )
    }

    private fun ResultRow.toSaleRecord(): SaleRecord =
        SaleRecord(
            id = this[SalesTable.id],
            productId = this[SalesTable.productId],
            clientId = this[SalesTable.clientId],
            clientName = this[ClientsTable.name],
            quantitySold = this[SalesTable.quantitySold],
            totalAmount = this[SalesTable.totalAmount],
            unitPrice = this[SalesTable.unitPrice],
            soldAtEpochMs = this[SalesTable.soldAtEpochMs],
            notes = this[SalesTable.notes],
            snapshotProductName = this[SalesTable.snapshotProductName],
            snapshotProductLine = this[SalesTable.snapshotProductLine],
            snapshotStage = ProductStage.fromDb(this[SalesTable.snapshotStage]),
            snapshotWasFailed = this[SalesTable.snapshotWasFailed],
            snapshotProviderName = this[SalesTable.snapshotProviderName],
            snapshotAcquisitionCostTotal = this[SalesTable.snapshotAcquisitionCostTotal],
            snapshotAcquisitionMaterialTotal = this[SalesTable.snapshotAcquisitionMaterialTotal],
            snapshotAcquisitionTransportTotal = this[SalesTable.snapshotAcquisitionTransportTotal],
            snapshotProcessingCostTotal = this[SalesTable.snapshotProcessingCostTotal],
            snapshotUnitCostBasis = this[SalesTable.snapshotUnitCostBasis],
            snapshotMarginPercent = this[SalesTable.snapshotMarginPercent],
            snapshotSuggestedTotal = this[SalesTable.snapshotSuggestedTotal],
        )

    private fun transportCostSumForProduct(productId: Int): Double =
        AcquisitionTransportCostsTable
            .selectAll()
            .where { AcquisitionTransportCostsTable.productId eq productId }
            .sumOf { it[AcquisitionTransportCostsTable.lineCost] }

    private fun landedAcquisitionPerPole(
        materialPerPole: Double?,
        lotQuantity: Double,
        transportTotalForLot: Double,
    ): Double {
        val m = materialPerPole ?: 0.0
        val t = if (lotQuantity > 1e-12) transportTotalForLot / lotQuantity else 0.0
        return m + t
    }

    private fun ResultRow.toAcquisitionTransportLine(): AcquisitionTransportLine =
        AcquisitionTransportLine(
            id = this[AcquisitionTransportCostsTable.id],
            productId = this[AcquisitionTransportCostsTable.productId],
            label = this[AcquisitionTransportCostsTable.label],
            lineCost = this[AcquisitionTransportCostsTable.lineCost],
            notes = this[AcquisitionTransportCostsTable.notes],
            createdAtEpochMs = this[AcquisitionTransportCostsTable.createdAtEpochMs],
        )
}
