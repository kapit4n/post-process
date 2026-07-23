package com.inventory.industry.data

import com.inventory.industry.domain.PoleStorageLocation
import com.inventory.industry.domain.ProductStage
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class InventoryCrudTests {

    private lateinit var repo: InventoryRepository

    @BeforeEach
    fun setup() {
        TestDatabaseHelper.withCleanDb {}
        repo = InventoryRepository()
    }

    // ── Catalog Products ──────────────────────────────────────────────

    @Nested
    inner class CatalogProductCrud {
        @Test
        fun `create catalog product returns positive id`() {
            val id = repo.upsertCatalogProduct(
                id = null, name = "Poste Pino 8m", productLine = "Distribución",
                description = "Test description",
            )
            assertTrue(id > 0)
        }

        @Test
        fun `list catalog products returns created items`() {
            repo.upsertCatalogProduct(id = null, name = "Poste A", productLine = "Línea A", description = null)
            repo.upsertCatalogProduct(id = null, name = "Poste B", productLine = "Línea B", description = null)

            val list = repo.listCatalogProducts()
            assertEquals(2, list.size)
            assertEquals("Poste A", list[0].name)
            assertEquals("Poste B", list[1].name)
        }

        @Test
        fun `list catalog products are ordered by name`() {
            repo.upsertCatalogProduct(id = null, name = "Zebra", productLine = "L1", description = null)
            repo.upsertCatalogProduct(id = null, name = "Alpha", productLine = "L2", description = null)
            repo.upsertCatalogProduct(id = null, name = "Mike", productLine = "L3", description = null)

            val list = repo.listCatalogProducts()
            assertEquals("Alpha", list[0].name)
            assertEquals("Mike", list[1].name)
            assertEquals("Zebra", list[2].name)
        }

        @Test
        fun `update catalog product modifies fields`() {
            val id = repo.upsertCatalogProduct(
                id = null, name = "Old Name", productLine = "Old Line", description = "Old",
            )
            repo.upsertCatalogProduct(id = id, name = "New Name", productLine = "New Line", description = "New")

            val item = repo.listCatalogProducts().first { it.id == id }
            assertEquals("New Name", item.name)
            assertEquals("New Line", item.productLine)
            assertEquals("New", item.description)
        }

        @Test
        fun `delete catalog product removes it`() {
            val id = repo.upsertCatalogProduct(id = null, name = "ToDelete", productLine = "L", description = null)
            repo.deleteCatalogProduct(id)

            assertTrue(repo.listCatalogProducts().isEmpty())
        }

        @Test
        fun `delete catalog product nullifies references in products`() {
            val catId = repo.upsertCatalogProduct(id = null, name = "Cat", productLine = "L", description = null)
            val prodId = repo.upsertProduct(
                id = null, name = "Prod", productLine = "L", stage = ProductStage.CRUDO,
                quantity = 10.0, notes = null, catalogProductId = catId, providerId = null,
                standardSalePrice = null, failedSalePrice = null, acquisitionCostPerPole = null,
            )
            repo.deleteCatalogProduct(catId)

            val prod = repo.getProduct(prodId)
            assertNotNull(prod)
            assertNull(prod.catalogProductId)
        }

        @Test
        fun `create catalog product with null description`() {
            val id = repo.upsertCatalogProduct(
                id = null, name = "Poste", productLine = "L", description = null,
            )
            val item = repo.listCatalogProducts().first { it.id == id }
            assertNull(item.description)
        }
    }

    // ── Products ──────────────────────────────────────────────────────

    @Nested
    inner class ProductCrud {
        @Test
        fun `create product returns positive id`() {
            val id = repo.upsertProduct(
                id = null, name = "Lote Test", productLine = "Línea",
                stage = ProductStage.CRUDO, quantity = 100.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = 90_000.0, failedSalePrice = 30_000.0,
                acquisitionCostPerPole = 25_000.0,
            )
            assertTrue(id > 0)
        }

        @Test
        fun `list products returns all`() {
            repo.upsertProduct(
                id = null, name = "Lote A", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 50.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
            )
            repo.upsertProduct(
                id = null, name = "Lote B", productLine = "L",
                stage = ProductStage.TERMINADO, quantity = 30.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
            )

            assertEquals(2, repo.listProducts().size)
        }

        @Test
        fun `list products by stage filters correctly`() {
            repo.upsertProduct(
                id = null, name = "Crudo", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 50.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
            )
            repo.upsertProduct(
                id = null, name = "Terminado", productLine = "L",
                stage = ProductStage.TERMINADO, quantity = 30.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
            )

            val crudos = repo.listProducts(ProductStage.CRUDO)
            assertEquals(1, crudos.size)
            assertEquals("Crudo", crudos[0].name)
        }

        @Test
        fun `get product by id returns correct product`() {
            val id = repo.upsertProduct(
                id = null, name = "Lote X", productLine = "LX",
                stage = ProductStage.DESCORTEZADO, quantity = 40.0, notes = "nota",
                catalogProductId = null, providerId = null,
                standardSalePrice = 80_000.0, failedSalePrice = 25_000.0,
                acquisitionCostPerPole = 27_000.0,
            )
            val prod = repo.getProduct(id)
            assertNotNull(prod)
            assertEquals("Lote X", prod.name)
            assertEquals("LX", prod.productLine)
            assertEquals(ProductStage.DESCORTEZADO, prod.stage)
            assertEquals(40.0, prod.quantity)
            assertEquals("nota", prod.notes)
            assertEquals(80_000.0, prod.standardSalePrice)
            assertEquals(25_000.0, prod.failedSalePrice)
            assertEquals(27_000.0, prod.acquisitionCostPerPole)
        }

        @Test
        fun `get product with invalid id returns null`() {
            assertNull(repo.getProduct(99999))
        }

        @Test
        fun `update product modifies all fields`() {
            val id = repo.upsertProduct(
                id = null, name = "Old", productLine = "OL",
                stage = ProductStage.CRUDO, quantity = 10.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
            )
            repo.upsertProduct(
                id = id, name = "New", productLine = "NL",
                stage = ProductStage.TRATADO, quantity = 20.0, notes = "updated",
                catalogProductId = null, providerId = null,
                standardSalePrice = 100_000.0, failedSalePrice = 40_000.0,
                acquisitionCostPerPole = 30_000.0,
            )

            val prod = repo.getProduct(id)!!
            assertEquals("New", prod.name)
            assertEquals("NL", prod.productLine)
            assertEquals(ProductStage.TRATADO, prod.stage)
            assertEquals(20.0, prod.quantity)
            assertEquals("updated", prod.notes)
            assertEquals(100_000.0, prod.standardSalePrice)
            assertEquals(40_000.0, prod.failedSalePrice)
            assertEquals(30_000.0, prod.acquisitionCostPerPole)
        }

        @Test
        fun `delete product removes it`() {
            val id = repo.upsertProduct(
                id = null, name = "ToDelete", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 10.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
            )
            repo.deleteProduct(id)
            assertNull(repo.getProduct(id))
        }

        @Test
        fun `product with provider shows provider name`() {
            val provId = repo.upsertPoleProvider(id = null, name = "Proveedor ABC", contact = null, notes = null)
            val prodId = repo.upsertProduct(
                id = null, name = "Lote", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 10.0, notes = null,
                catalogProductId = null, providerId = provId,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
            )
            val prod = repo.getProduct(prodId)!!
            assertEquals("Proveedor ABC", prod.providerName)
        }

        @Test
        fun `product without provider has null provider name`() {
            val id = repo.upsertProduct(
                id = null, name = "Lote", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 10.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
            )
            val prod = repo.getProduct(id)!!
            assertNull(prod.providerName)
        }

        @Test
        fun `product defaults to FABRICA location`() {
            val id = repo.upsertProduct(
                id = null, name = "Lote", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 10.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
            )
            val prod = repo.getProduct(id)!!
            assertEquals(PoleStorageLocation.FABRICA, prod.acquisitionStorageLocation)
        }

        @Test
        fun `product with EN_PROVEEDOR location`() {
            val id = repo.upsertProduct(
                id = null, name = "Lote", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 10.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
                acquisitionStorageLocation = PoleStorageLocation.EN_PROVEEDOR,
            )
            val prod = repo.getProduct(id)!!
            assertEquals(PoleStorageLocation.EN_PROVEEDOR, prod.acquisitionStorageLocation)
        }
    }

    // ── Providers ─────────────────────────────────────────────────────

    @Nested
    inner class ProviderCrud {
        @Test
        fun `create provider returns positive id`() {
            val id = repo.upsertPoleProvider(id = null, name = "Prov", contact = null, notes = null)
            assertTrue(id > 0)
        }

        @Test
        fun `list providers returns all`() {
            repo.upsertPoleProvider(id = null, name = "A", contact = null, notes = null)
            repo.upsertPoleProvider(id = null, name = "B", contact = null, notes = null)
            assertEquals(2, repo.listPoleProviders().size)
        }

        @Test
        fun `update provider modifies fields`() {
            val id = repo.upsertPoleProvider(id = null, name = "Old", contact = null, notes = null)
            repo.upsertPoleProvider(id = id, name = "New", contact = "123", notes = "note")
            val prov = repo.listPoleProviders().first { it.id == id }
            assertEquals("New", prov.name)
            assertEquals("123", prov.contact)
            assertEquals("note", prov.notes)
        }

        @Test
        fun `delete provider removes it`() {
            val id = repo.upsertPoleProvider(id = null, name = "Del", contact = null, notes = null)
            repo.deletePoleProvider(id)
            assertTrue(repo.listPoleProviders().isEmpty())
        }

        @Test
        fun `delete provider nullifies product references`() {
            val provId = repo.upsertPoleProvider(id = null, name = "Prov", contact = null, notes = null)
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 10.0, notes = null,
                catalogProductId = null, providerId = provId,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
            )
            repo.deletePoleProvider(provId)
            val prod = repo.getProduct(prodId)!!
            assertNull(prod.providerId)
        }
    }

    // ── Clients ───────────────────────────────────────────────────────

    @Nested
    inner class ClientCrud {
        @Test
        fun `create client returns positive id`() {
            val id = repo.upsertClient(id = null, name = "Client", contact = null, notes = null)
            assertTrue(id > 0)
        }

        @Test
        fun `list clients returns all`() {
            repo.upsertClient(id = null, name = "A", contact = null, notes = null)
            repo.upsertClient(id = null, name = "B", contact = null, notes = null)
            assertEquals(2, repo.listClients().size)
        }

        @Test
        fun `update client modifies fields`() {
            val id = repo.upsertClient(id = null, name = "Old", contact = null, notes = null)
            repo.upsertClient(id = id, name = "New", contact = "abc", notes = "n")
            val client = repo.listClients().first { it.id == id }
            assertEquals("New", client.name)
            assertEquals("abc", client.contact)
            assertEquals("n", client.notes)
        }

        @Test
        fun `delete client removes it when no sales`() {
            val id = repo.upsertClient(id = null, name = "Del", contact = null, notes = null)
            val deleted = repo.deleteClient(id)
            assertTrue(deleted)
            assertTrue(repo.listClients().isEmpty())
        }

        @Test
        fun `delete client fails when has sales`() {
            val clientId = repo.upsertClient(id = null, name = "C", contact = null, notes = null)
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.TERMINADO, quantity = 10.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = 100.0, failedSalePrice = null,
                acquisitionCostPerPole = null,
            )
            repo.recordSale(
                productId = prodId, clientId = clientId, quantitySold = 5.0,
                totalAmount = 500.0, soldAtEpochMs = System.currentTimeMillis(),
                notes = null,
            )
            val deleted = repo.deleteClient(clientId)
            assertEquals(false, deleted)
        }
    }

    // ── Drivers ───────────────────────────────────────────────────────

    @Nested
    inner class DriverCrud {
        @Test
        fun `create driver returns positive id`() {
            val id = repo.upsertDriver(id = null, name = "Driver", phone = null, notes = null)
            assertTrue(id > 0)
        }

        @Test
        fun `list drivers returns all`() {
            repo.upsertDriver(id = null, name = "A", phone = null, notes = null)
            repo.upsertDriver(id = null, name = "B", phone = null, notes = null)
            assertEquals(2, repo.listDrivers().size)
        }

        @Test
        fun `driver name is trimmed on insert`() {
            val id = repo.upsertDriver(id = null, name = "  Juan  ", phone = null, notes = null)
            val driver = repo.listDrivers().first { it.id == id }
            assertEquals("Juan", driver.name)
        }

        @Test
        fun `blank phone is stored as null`() {
            val id = repo.upsertDriver(id = null, name = "D", phone = "   ", notes = null)
            val driver = repo.listDrivers().first { it.id == id }
            assertNull(driver.phone)
        }

        @Test
        fun `delete driver succeeds when not used`() {
            val id = repo.upsertDriver(id = null, name = "D", phone = null, notes = null)
            val deleted = repo.deleteDriver(id)
            assertTrue(deleted)
        }

        @Test
        fun `delete driver fails when used in transport run`() {
            val driverId = repo.upsertDriver(id = null, name = "D", phone = null, notes = null)
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 10.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
                acquisitionStorageLocation = PoleStorageLocation.EN_PROVEEDOR,
            )
            repo.startProviderTransport(
                driverId = driverId, vehiclePlate = "AA-BB-11",
                freightCost = 100_000.0, gruaCost = 0.0,
                departedAtEpochMs = System.currentTimeMillis(),
                expectedArrivalEpochMs = null,
                productIds = listOf(prodId), notes = null,
            )
            val deleted = repo.deleteDriver(driverId)
            assertEquals(false, deleted)
        }
    }

    // ── Resources ─────────────────────────────────────────────────────

    @Nested
    inner class ResourceCrud {
        @Test
        fun `create resource returns positive id`() {
            val id = repo.upsertResource(id = null, name = "Agua", unit = "L", costPerUnit = 5.0)
            assertTrue(id > 0)
        }

        @Test
        fun `list resources returns all`() {
            repo.upsertResource(id = null, name = "Agua", unit = "L", costPerUnit = 5.0)
            repo.upsertResource(id = null, name = "Combustible", unit = "L", costPerUnit = 1200.0)
            assertEquals(2, repo.listResources().size)
        }

        @Test
        fun `update resource modifies fields`() {
            val id = repo.upsertResource(id = null, name = "Old", unit = "kg", costPerUnit = 100.0)
            repo.upsertResource(id = id, name = "New", unit = "L", costPerUnit = 200.0)
            val res = repo.listResources().first { it.id == id }
            assertEquals("New", res.name)
            assertEquals("L", res.unit)
            assertEquals(200.0, res.costPerUnit)
        }

        @Test
        fun `delete resource removes it`() {
            val id = repo.upsertResource(id = null, name = "Del", unit = "kg", costPerUnit = 10.0)
            repo.deleteResource(id)
            assertTrue(repo.listResources().isEmpty())
        }
    }

    // ── Inventory Flow Summary ────────────────────────────────────────

    @Nested
    inner class InventoryFlow {
        @Test
        fun `empty inventory returns zero summary`() {
            val summary = repo.inventoryFlowSummary()
            assertEquals(0.0, summary.polesInProcessOk)
            assertEquals(0.0, summary.polesReadyStandardSale)
            assertEquals(0.0, summary.polesFailedSalvage)
        }

        @Test
        fun `summary counts products by stage`() {
            repo.upsertProduct(
                id = null, name = "C1", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 50.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
            )
            repo.upsertProduct(
                id = null, name = "T1", productLine = "L",
                stage = ProductStage.TERMINADO, quantity = 30.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
            )
            TestDataBuilder.createProduct(
                name = "F1", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 5.0,
                isFailed = true, failedAtStage = ProductStage.CRUDO,
            )

            val summary = repo.inventoryFlowSummary()
            assertEquals(50.0, summary.polesInProcessOk)
            assertEquals(30.0, summary.polesReadyStandardSale)
            assertEquals(5.0, summary.polesFailedSalvage)
        }

        @Test
        fun `sellable products include terminado and failed`() {
            TestDataBuilder.createProduct(
                name = "OK", productLine = "L",
                stage = ProductStage.TERMINADO, quantity = 10.0,
            )
            TestDataBuilder.createProduct(
                name = "Failed", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 5.0,
                isFailed = true, failedAtStage = ProductStage.CRUDO,
            )
            TestDataBuilder.createProduct(
                name = "Crudo", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 20.0,
            )

            val sellable = repo.listSellableProducts()
            assertEquals(2, sellable.size)
        }
    }
}
