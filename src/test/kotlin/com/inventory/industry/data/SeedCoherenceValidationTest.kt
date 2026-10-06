package com.inventory.industry.data

import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SeedCoherenceValidationTest {

    @Test
    fun `realistic seed is coherent`() {
        TestDatabaseHelper.withCleanDb {
            seedDefaultResourcesIfEmpty()
            seedDefaultStageTemplatesIfEmpty()
            seedDemoInventoryDataIfEmpty()
            seedDemoResourceStockLotsIfEmpty()

            transaction {
                val resources = ResourcesTable.selectAll().count()
                val templates = StageResourceTemplatesTable.selectAll().count()
                val stockLots = ResourceStockLotsTable.selectAll().count()
                val catalog = CatalogProductsTable.selectAll().count()
                val providers = PoleProvidersTable.selectAll().count()
                val clients = ClientsTable.selectAll().count()
                val drivers = DriversTable.selectAll().count()
                val products = ProductsTable.selectAll().count()
                val transports = ProviderTransportRunsTable.selectAll().count()
                val transformations = TransformationsTable.selectAll().count()
                val sales = SalesTable.selectAll().count()
                val costs = ProcessCostsTable.selectAll().count()

                println("RESOURCES=$resources")
                println("TEMPLATES=$templates")
                println("STOCK_LOTS=$stockLots")
                println("CATALOG=$catalog")
                println("PROVIDERS=$providers")
                println("CLIENTS=$clients")
                println("DRIVERS=$drivers")
                println("PRODUCTS=$products")
                println("TRANSPORTS=$transports")
                println("TRANSFORMATIONS=$transformations")
                println("SALES=$sales")
                println("PROCESS_COSTS=$costs")

                assertTrue(resources >= 25)
                assertTrue(templates >= 15)
                assertTrue(stockLots >= 100)
                assertEquals(5, catalog)
                assertTrue(providers >= 20)
                assertTrue(clients >= 70)
                assertTrue(drivers >= 8)
                assertTrue(products > 60)
                assertTrue(transports > 20)
                assertTrue(transformations > 60)
                assertTrue(sales > 150)
                assertTrue(costs > 200)

                // No negative inventory.
                val neg = ProductsTable.selectAll().any { it[ProductsTable.quantity] < 0 }
                assertTrue(!neg, "no negative quantity")

                // Economía: ingresos vs. costo de lo vendido (snapshots por venta).
                val salesTotal = SalesTable.selectAll().sumOf { it[SalesTable.totalAmount] }
                val soldPoles = SalesTable.selectAll().sumOf { it[SalesTable.quantitySold] }
                val cogsTotal =
                    SalesTable.selectAll().sumOf {
                        it[SalesTable.snapshotAcquisitionCostTotal] +
                            it[SalesTable.snapshotAcquisitionTransportTotal] +
                            it[SalesTable.snapshotProcessingCostTotal]
                    }
                val onHandPoles = ProductsTable.selectAll().sumOf { it[ProductsTable.quantity] }
                val invByStage =
                    ProductsTable.selectAll()
                        .groupBy { it[ProductsTable.stage] }
                        .mapValues { (_, v) -> v.sumOf { it[ProductsTable.quantity] } }
                        .toSortedMap()
                println("SALES_TOTAL=$salesTotal")
                println("COGS_TOTAL=$cogsTotal")
                println("SOLD_POLES=$soldPoles")
                println("ON_HAND_POLES=$onHandPoles")
                println("TOTAL_POLES=${soldPoles + onHandPoles}")
                println("INV_BY_STAGE=$invByStage")
                assertTrue(salesTotal > 150_000.0)
                assertTrue(salesTotal > cogsTotal, "revenue should exceed COGS")
                assertTrue(salesTotal < cogsTotal * 2.5, "margin sanity")
                assertTrue(invByStage.values.none { it < 0 })
            }
        }
    }
}
