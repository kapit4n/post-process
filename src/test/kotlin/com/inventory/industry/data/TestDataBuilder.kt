package com.inventory.industry.data

import com.inventory.industry.domain.PoleStorageLocation
import com.inventory.industry.domain.ProductStage
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Reusable builders for test data. Each builder inserts a record and returns its ID.
 * Use inside a [transaction] block.
 */
object TestDataBuilder {

    fun createCatalogProduct(
        name: String = "Poste Test",
        productLine: String = "Línea Test",
        description: String? = "Descripción de prueba",
    ): Int =
        CatalogProductsTable.insert {
            it[CatalogProductsTable.name] = name
            it[CatalogProductsTable.productLine] = productLine
            it[CatalogProductsTable.description] = description
            it[CatalogProductsTable.createdAtEpochMs] = System.currentTimeMillis()
        }[CatalogProductsTable.id]

    fun createProvider(
        name: String = "Proveedor Test",
        contact: String? = "+56 9 1234 5678",
        notes: String? = null,
    ): Int =
        PoleProvidersTable.insert {
            it[PoleProvidersTable.name] = name
            it[PoleProvidersTable.contact] = contact
            it[PoleProvidersTable.notes] = notes
            it[PoleProvidersTable.createdAtEpochMs] = System.currentTimeMillis()
        }[PoleProvidersTable.id]

    fun createClient(
        name: String = "Cliente Test",
        contact: String? = "+56 9 8765 4321",
        notes: String? = null,
    ): Int =
        ClientsTable.insert {
            it[ClientsTable.name] = name
            it[ClientsTable.contact] = contact
            it[ClientsTable.notes] = notes
            it[ClientsTable.createdAtEpochMs] = System.currentTimeMillis()
        }[ClientsTable.id]

    fun createDriver(
        name: String = "Chofer Test",
        phone: String? = "+56 9 1111 2222",
        notes: String? = null,
    ): Int =
        DriversTable.insert {
            it[DriversTable.name] = name
            it[DriversTable.phone] = phone
            it[DriversTable.notes] = notes
            it[DriversTable.createdAtEpochMs] = System.currentTimeMillis()
        }[DriversTable.id]

    fun createResource(
        name: String = "Insumo Test",
        unit: String = "kg",
        costPerUnit: Double = 1_000.0,
    ): Int =
        ResourcesTable.insert {
            it[ResourcesTable.name] = name
            it[ResourcesTable.unit] = unit
            it[ResourcesTable.costPerUnit] = costPerUnit
        }[ResourcesTable.id]

    fun createProduct(
        name: String = "Lote Test",
        productLine: String = "Línea Test",
        stage: ProductStage = ProductStage.CRUDO,
        quantity: Double = 50.0,
        catalogProductId: Int? = null,
        providerId: Int? = null,
        standardSalePrice: Double? = 95_000.0,
        failedSalePrice: Double? = 35_000.0,
        acquisitionCostPerPole: Double? = 25_000.0,
        acquisitionStorageLocation: PoleStorageLocation = PoleStorageLocation.FABRICA,
        isFailed: Boolean = false,
        failedAtStage: ProductStage? = null,
    ): Int = transaction {
        ProductsTable.insert {
            it[ProductsTable.name] = name
            it[ProductsTable.productLine] = productLine
            it[ProductsTable.stage] = stage.name
            it[ProductsTable.quantity] = quantity
            it[ProductsTable.notes] = null
            it[ProductsTable.createdAtEpochMs] = System.currentTimeMillis()
            it[ProductsTable.catalogProductId] = catalogProductId
            it[ProductsTable.providerId] = providerId
            it[ProductsTable.isFailed] = isFailed
            it[ProductsTable.failedAtStage] = failedAtStage?.name
            it[ProductsTable.standardSalePrice] = standardSalePrice
            it[ProductsTable.failedSalePrice] = failedSalePrice
            it[ProductsTable.acquisitionCostPerPole] = acquisitionCostPerPole
            it[ProductsTable.acquisitionStorageLocation] = acquisitionStorageLocation.name
        }[ProductsTable.id]
    }

    fun createTransportCost(
        productId: Int,
        label: String = "Flete",
        lineCost: Double = 500_000.0,
    ): Int =
        AcquisitionTransportCostsTable.insert {
            it[AcquisitionTransportCostsTable.productId] = productId
            it[AcquisitionTransportCostsTable.label] = label
            it[AcquisitionTransportCostsTable.lineCost] = lineCost
            it[AcquisitionTransportCostsTable.notes] = null
            it[AcquisitionTransportCostsTable.createdAtEpochMs] = System.currentTimeMillis()
        }[AcquisitionTransportCostsTable.id]

    fun createStageResourceTemplate(
        fromStage: ProductStage = ProductStage.CRUDO,
        resourceId: Int,
        amountPerPole: Double = 10.0,
        displayOrder: Int = 1,
        notes: String? = null,
    ): Int =
        StageResourceTemplatesTable.insert {
            it[StageResourceTemplatesTable.fromStage] = fromStage.name
            it[StageResourceTemplatesTable.resourceId] = resourceId
            it[StageResourceTemplatesTable.amountPerPole] = amountPerPole
            it[StageResourceTemplatesTable.notes] = notes
            it[StageResourceTemplatesTable.displayOrder] = displayOrder
        }[StageResourceTemplatesTable.id]

    fun createProcessCost(
        productId: Int,
        transformationId: Int? = null,
        fromStage: ProductStage = ProductStage.CRUDO,
        toStage: ProductStage = ProductStage.DESCORTEZADO,
        resourceId: Int? = null,
        amountUsed: Double? = 10.0,
        lineCost: Double = 10_000.0,
        label: String = "Test cost",
    ): Int = transaction {
        ProcessCostsTable.insert {
            it[ProcessCostsTable.productId] = productId
            it[ProcessCostsTable.transformationId] = transformationId
            it[ProcessCostsTable.fromStage] = fromStage.name
            it[ProcessCostsTable.toStage] = toStage.name
            it[ProcessCostsTable.resourceId] = resourceId
            it[ProcessCostsTable.amountUsed] = amountUsed
            it[ProcessCostsTable.lineCost] = lineCost
            it[ProcessCostsTable.label] = label
            it[ProcessCostsTable.createdAtEpochMs] = System.currentTimeMillis()
        }[ProcessCostsTable.id]
    }

    /**
     * Creates a full CRUDO product with a provider and catalog product already linked.
     * Returns Triple(catalogProductId, providerId, productId).
     */
    fun createFullCrudoProduct(
        name: String = "Lote Pino Test",
        quantity: Double = 50.0,
        acquisitionCostPerPole: Double = 25_000.0,
        location: PoleStorageLocation = PoleStorageLocation.FABRICA,
    ): Triple<Int, Int, Int> = transaction {
        val catId = createCatalogProduct(name = "Catálogo $name")
        val provId = createProvider(name = "Proveedor $name")
        val prodId = createProduct(
            name = name,
            quantity = quantity,
            stage = ProductStage.CRUDO,
            catalogProductId = catId,
            providerId = provId,
            acquisitionCostPerPole = acquisitionCostPerPole,
            acquisitionStorageLocation = location,
        )
        Triple(catId, provId, prodId)
    }
}
