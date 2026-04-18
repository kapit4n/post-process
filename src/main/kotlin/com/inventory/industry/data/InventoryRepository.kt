package com.inventory.industry.data

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

            val acqPer = prow[ProductsTable.acquisitionCostPerPole]
            val procTotalOnLot =
                ProcessCostsTable
                    .selectAll()
                    .where { ProcessCostsTable.productId eq productId }
                    .sumOf { it[ProcessCostsTable.lineCost] }
            val procPerPole = if (qtyAvail > 1e-12) procTotalOnLot / qtyAvail else 0.0
            val acqPortion = quantitySold * (acqPer ?: 0.0)
            val procPortion = quantitySold * procPerPole
            val unitBasis = (acqPer ?: 0.0) + procPerPole
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
                }
                id
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
            val invAcq =
                ProductsTable
                    .selectAll()
                    .sumOf { r ->
                        r[ProductsTable.quantity] * (r[ProductsTable.acquisitionCostPerPole] ?: 0.0)
                    }
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
            AccountingCostOverview(
                totalProcessingCostAllTime = allTime,
                processingCostAttributedToOpenStock = onOpen,
                inventoryAcquisitionCostTotal = invAcq,
                soldAcquisitionCostTotal = soldAcq,
                soldProcessingCostTotal = soldProc,
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
     * Vista previa para venta: costo por poste (adquisición + proceso prorrateado) y precio sugerido.
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
            val acq = row[ProductsTable.acquisitionCostPerPole]
            val procTotal =
                ProcessCostsTable
                    .selectAll()
                    .where { ProcessCostsTable.productId eq productId }
                    .sumOf { it[ProcessCostsTable.lineCost] }
            val procPerPole = procTotal / qtyAvail
            val unitBasis = (acq ?: 0.0) + procPerPole
            val sugUnit = unitBasis * (1.0 + marginPercent / 100.0)
            SaleCostPreview(
                quantityAvailable = qtyAvail,
                acquisitionCostPerPole = acq,
                processingCostTotalOnLot = procTotal,
                processingCostPerPole = procPerPole,
                unitCostBasis = unitBasis,
                acquisitionTotalForSaleQty = quantitySold * (acq ?: 0.0),
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

            val mergedByBatch: Map<Int, Double> =
                inputs.groupBy { it.sourceProductId }
                    .mapValues { (_, list) -> list.sumOf { it.quantity } }
            val sources =
                mergedByBatch.map { (pid, qty) ->
                    val row =
                        ProductsTable
                            .selectAll()
                            .where { ProductsTable.id eq pid }
                            .singleOrNull()
                            ?: return@transaction TransformationResult.Err(
                                "Lote origen $pid no existe.",
                            )
                    val stage = ProductStage.fromDb(row[ProductsTable.stage])
                    if (stage != fromStage) {
                        return@transaction TransformationResult.Err(
                            "El lote '${row[ProductsTable.name]}' no está en ${fromStage.shortCode}.",
                        )
                    }
                    if (row[ProductsTable.isFailed]) {
                        return@transaction TransformationResult.Err(
                            "El lote '${row[ProductsTable.name]}' está marcado como fallado.",
                        )
                    }
                    val available = row[ProductsTable.quantity]
                    if (qty - available > 1e-6) {
                        return@transaction TransformationResult.Err(
                            "El lote '${row[ProductsTable.name]}' sólo tiene ${fmt(available)} " +
                                "disponibles (pidió ${fmt(qty)}).",
                        )
                    }
                    row to qty
                }

            val now = System.currentTimeMillis()
            val transformationId =
                TransformationsTable.insert {
                    it[TransformationsTable.fromStage] = fromStage.name
                    it[TransformationsTable.toStage] = toStage.name
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
                    takeQty * (row[ProductsTable.acquisitionCostPerPole] ?: 0.0)
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

    fun listTransformations(limit: Int = 200): List<Transformation> =
        transaction {
            val trows =
                TransformationsTable
                    .selectAll()
                    .orderBy(TransformationsTable.processedAtEpochMs to SortOrder.DESC)
                    .limit(limit)
                    .toList()
            if (trows.isEmpty()) return@transaction emptyList()

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

            trows.map { row ->
                val id = row[TransformationsTable.id]
                Transformation(
                    id = id,
                    fromStage = ProductStage.fromDb(row[TransformationsTable.fromStage]),
                    toStage = ProductStage.fromDb(row[TransformationsTable.toStage]),
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

    private fun fmt(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else "%.2f".format(v)

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
            snapshotProcessingCostTotal = this[SalesTable.snapshotProcessingCostTotal],
            snapshotUnitCostBasis = this[SalesTable.snapshotUnitCostBasis],
            snapshotMarginPercent = this[SalesTable.snapshotMarginPercent],
            snapshotSuggestedTotal = this[SalesTable.snapshotSuggestedTotal],
        )
}
