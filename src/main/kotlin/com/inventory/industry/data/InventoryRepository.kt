package com.inventory.industry.data

import com.inventory.industry.domain.ProductStage
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
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
            ProductsTable
                .selectAll()
                .orderBy(ProductsTable.name to SortOrder.ASC)
                .map { it.toProduct() }
        }

    fun listProducts(stage: ProductStage): List<Product> =
        transaction {
            ProductsTable
                .selectAll()
                .where { ProductsTable.stage eq stage.name }
                .orderBy(ProductsTable.name to SortOrder.ASC)
                .map { it.toProduct() }
        }

    fun getProduct(id: Int): Product? =
        transaction {
            ProductsTable
                .selectAll()
                .where { ProductsTable.id eq id }
                .singleOrNull()
                ?.toProduct()
        }

    fun upsertProduct(
        id: Int?,
        name: String,
        productLine: String,
        stage: ProductStage,
        quantity: Double,
        notes: String?,
        catalogProductId: Int?,
        standardSalePrice: Double?,
        failedSalePrice: Double?,
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
                    it[ProductsTable.isFailed] = false
                    it[ProductsTable.failedAtStage] = null
                    it[ProductsTable.standardSalePrice] = standardSalePrice
                    it[ProductsTable.failedSalePrice] = failedSalePrice
                }[ProductsTable.id]
            } else {
                ProductsTable.update({ ProductsTable.id eq id }) {
                    it[ProductsTable.name] = name
                    it[ProductsTable.productLine] = productLine
                    it[ProductsTable.stage] = stage.name
                    it[ProductsTable.quantity] = quantity
                    it[ProductsTable.notes] = notes
                    it[ProductsTable.catalogProductId] = catalogProductId
                    it[ProductsTable.standardSalePrice] = standardSalePrice
                    it[ProductsTable.failedSalePrice] = failedSalePrice
                }
                id
            }
        }

    fun deleteProduct(id: Int) {
        transaction {
            ProcessCostsTable.deleteWhere { ProcessCostsTable.productId eq id }
            ProductsTable.deleteWhere { ProductsTable.id eq id }
        }
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
            val inheritedStandardPrice = first[ProductsTable.standardSalePrice]
            val inheritedFailedPrice = first[ProductsTable.failedSalePrice]

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

            if (successCount > 0.0) {
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
                    it[ProductsTable.isFailed] = false
                    it[ProductsTable.failedAtStage] = null
                    it[ProductsTable.standardSalePrice] = inheritedStandardPrice
                    it[ProductsTable.failedSalePrice] = inheritedFailedPrice
                }
            }

            if (failedCount > 0.0) {
                ProductsTable.insert {
                    it[ProductsTable.name] = inheritedName
                    it[ProductsTable.productLine] = inheritedLine
                    it[ProductsTable.stage] = fromStage.name
                    it[ProductsTable.quantity] = failedCount
                    it[ProductsTable.notes] =
                        "Fallado durante la transformación #$transformationId en ${fromStage.shortCode}"
                    it[ProductsTable.createdAtEpochMs] = now
                    it[ProductsTable.catalogProductId] = inheritedCatalog
                    it[ProductsTable.isFailed] = true
                    it[ProductsTable.failedAtStage] = fromStage.name
                    it[ProductsTable.standardSalePrice] = inheritedStandardPrice
                    it[ProductsTable.failedSalePrice] = inheritedFailedPrice
                }
            }

            val resourcesById =
                ResourcesTable
                    .selectAll()
                    .associateBy { it[ResourcesTable.id] }

            resourceUses.forEach { u ->
                val r = resourcesById[u.resourceId] ?: return@forEach
                val cpu = r[ResourcesTable.costPerUnit]
                ProcessCostsTable.insert {
                    it[ProcessCostsTable.productId] = first[ProductsTable.id]
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
            isFailed = this[ProductsTable.isFailed],
            failedAtStage = failedAtRaw?.let { ProductStage.fromDb(it) },
            standardSalePrice = this[ProductsTable.standardSalePrice],
            failedSalePrice = this[ProductsTable.failedSalePrice],
        )
    }
}
