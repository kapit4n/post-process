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
                .map {
                    ProcessCostLine(
                        id = it[ProcessCostsTable.id],
                        productId = it[ProcessCostsTable.productId],
                        fromStage = ProductStage.fromDb(it[ProcessCostsTable.fromStage]),
                        toStage = ProductStage.fromDb(it[ProcessCostsTable.toStage]),
                        resourceId = it[ProcessCostsTable.resourceId],
                        resourceName = it.getOrNull(ResourcesTable.name),
                        amountUsed = it[ProcessCostsTable.amountUsed],
                        lineCost = it[ProcessCostsTable.lineCost],
                        label = it[ProcessCostsTable.label],
                        createdAtEpochMs = it[ProcessCostsTable.createdAtEpochMs],
                    )
                }
        }

    data class ResourceUse(
        val resourceId: Int,
        val amount: Double,
        val label: String,
    )

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

    fun advanceWithCosts(
        productId: Int,
        uses: List<ResourceUse>,
    ): Boolean =
        transaction {
            val row =
                ProductsTable
                    .selectAll()
                    .where { ProductsTable.id eq productId }
                    .singleOrNull() ?: return@transaction false
            if (row[ProductsTable.isFailed]) return@transaction false
            val current = ProductStage.fromDb(row[ProductsTable.stage])
            val next = current.next() ?: return@transaction false
            val resourcesById =
                ResourcesTable
                    .selectAll()
                    .associateBy { it[ResourcesTable.id] }
            val now = System.currentTimeMillis()
            if (uses.isNotEmpty()) {
                var any = false
                uses.forEach { u ->
                    val r = resourcesById[u.resourceId] ?: return@forEach
                    any = true
                    val cpu = r[ResourcesTable.costPerUnit]
                    ProcessCostsTable.insert {
                        it[ProcessCostsTable.productId] = productId
                        it[ProcessCostsTable.fromStage] = current.name
                        it[ProcessCostsTable.toStage] = next.name
                        it[ProcessCostsTable.resourceId] = u.resourceId
                        it[ProcessCostsTable.amountUsed] = u.amount
                        it[ProcessCostsTable.lineCost] = u.amount * cpu
                        it[ProcessCostsTable.label] = u.label
                        it[ProcessCostsTable.createdAtEpochMs] = now
                    }
                }
                if (!any) return@transaction false
            } else {
                ProcessCostsTable.insert {
                    it[ProcessCostsTable.productId] = productId
                    it[ProcessCostsTable.fromStage] = current.name
                    it[ProcessCostsTable.toStage] = next.name
                    it[ProcessCostsTable.resourceId] = null
                    it[ProcessCostsTable.amountUsed] = null
                    it[ProcessCostsTable.lineCost] = 0.0
                    it[ProcessCostsTable.label] = "No resources logged"
                    it[ProcessCostsTable.createdAtEpochMs] = now
                }
            }
            ProductsTable.update({ ProductsTable.id eq productId }) {
                it[ProductsTable.stage] = next.name
            }
            true
        }

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
