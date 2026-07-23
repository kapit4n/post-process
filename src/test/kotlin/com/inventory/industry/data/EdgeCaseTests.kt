package com.inventory.industry.data

import com.inventory.industry.domain.PoleStorageLocation
import com.inventory.industry.domain.ProductStage
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class EdgeCaseTests {

    private lateinit var repo: InventoryRepository

    @BeforeEach
    fun setup() {
        TestDatabaseHelper.withCleanDb {}
        repo = InventoryRepository()
    }

    // ── Empty Inventory ───────────────────────────────────────────────

    @Nested
    inner class EmptyInventory {
        @Test
        fun `list products on empty db returns empty`() {
            assertTrue(repo.listProducts().isEmpty())
        }

        @Test
        fun `list sellable products on empty db returns empty`() {
            assertTrue(repo.listSellableProducts().isEmpty())
        }

        @Test
        fun `inventory flow summary on empty db returns zeros`() {
            val summary = repo.inventoryFlowSummary()
            assertEquals(0.0, summary.polesInProcessOk)
            assertEquals(0.0, summary.polesReadyStandardSale)
            assertEquals(0.0, summary.polesFailedSalvage)
            assertTrue(summary.perStage.all { it.totalPoles == 0.0 && it.lotCount == 0 })
        }

        @Test
        fun `recent dashboard activity on empty db returns empty`() {
            assertTrue(repo.recentDashboardActivity().isEmpty())
        }

        @Test
        fun `list sales on empty db returns empty`() {
            assertTrue(repo.listSales().isEmpty())
        }

        @Test
        fun `list transformations on empty db returns empty`() {
            assertTrue(repo.listTransformations().isEmpty())
        }

        @Test
        fun `list providers on empty db returns empty`() {
            assertTrue(repo.listPoleProviders().isEmpty())
        }

        @Test
        fun `list clients on empty db returns empty`() {
            assertTrue(repo.listClients().isEmpty())
        }

        @Test
        fun `list drivers on empty db returns empty`() {
            assertTrue(repo.listDrivers().isEmpty())
        }

        @Test
        fun `list resources on empty db returns empty`() {
            assertTrue(repo.listResources().isEmpty())
        }
    }

    // ── Zero Quantities ───────────────────────────────────────────────

    @Nested
    inner class ZeroQuantities {
        @Test
        fun `product with zero quantity can be created`() {
            val id = repo.upsertProduct(
                id = null, name = "Zero", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 0.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
            )
            val prod = repo.getProduct(id)!!
            assertEquals(0.0, prod.quantity)
        }

        @Test
        fun `product with very small quantity is not sellable`() {
            val id = repo.upsertProduct(
                id = null, name = "Tiny", productLine = "L",
                stage = ProductStage.TERMINADO, quantity = 1e-8, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = 100.0, failedSalePrice = null,
                acquisitionCostPerPole = null,
            )
            val prod = repo.getProduct(id)!!
            assertFalse(prod.isSellable() && prod.quantity > 1e-6)
        }
    }

    // ── Invalid IDs ───────────────────────────────────────────────────

    @Nested
    inner class InvalidIds {
        @Test
        fun `get product with negative id returns null`() {
            assertNull(repo.getProduct(-1))
        }

        @Test
        fun `get product with zero id returns null`() {
            assertNull(repo.getProduct(0))
        }

        @Test
        fun `delete product with non-existent id does not throw`() {
            repo.deleteProduct(99999)
        }

        @Test
        fun `delete catalog product with non-existent id does not throw`() {
            repo.deleteCatalogProduct(99999)
        }

        @Test
        fun `delete provider with non-existent id does not throw`() {
            repo.deletePoleProvider(99999)
        }

        @Test
        fun `delete resource with non-existent id does not throw`() {
            repo.deleteResource(99999)
        }
    }

    // ── Large Values ──────────────────────────────────────────────────

    @Nested
    inner class LargeValues {
        @Test
        fun `product with very large quantity`() {
            val id = repo.upsertProduct(
                id = null, name = "Huge", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 1_000_000.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
            )
            val prod = repo.getProduct(id)!!
            assertEquals(1_000_000.0, prod.quantity)
        }

        @Test
        fun `product with very large cost per pole`() {
            val id = repo.upsertProduct(
                id = null, name = "Expensive", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 1.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = 999_999_999.99,
            )
            val prod = repo.getProduct(id)!!
            assertEquals(999_999_999.99, prod.acquisitionCostPerPole!!)
        }

        @Test
        fun `resource with very large cost`() {
            val id = repo.upsertResource(
                id = null, name = "MegaResource", unit = "kg",
                costPerUnit = 10_000_000.0,
            )
            val res = repo.listResources().first { it.id == id }
            assertEquals(10_000_000.0, res.costPerUnit)
        }
    }

    // ── Null Optional Fields ──────────────────────────────────────────

    @Nested
    inner class NullOptionalFields {
        @Test
        fun `product with all nullable fields`() {
            val id = repo.upsertProduct(
                id = null, name = "Nulls", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 10.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
            )
            val prod = repo.getProduct(id)!!
            assertNull(prod.notes)
            assertNull(prod.catalogProductId)
            assertNull(prod.providerId)
            assertNull(prod.standardSalePrice)
            assertNull(prod.failedSalePrice)
            assertNull(prod.acquisitionCostPerPole)
            assertNull(prod.failedAtStage)
        }

        @Test
        fun `provider with all nullable fields`() {
            val id = repo.upsertPoleProvider(id = null, name = "P", contact = null, notes = null)
            val prov = repo.listPoleProviders().first { it.id == id }
            assertNull(prov.contact)
            assertNull(prov.notes)
        }

        @Test
        fun `driver with all nullable fields`() {
            val id = repo.upsertDriver(id = null, name = "D", phone = null, notes = null)
            val driver = repo.listDrivers().first { it.id == id }
            assertNull(driver.phone)
            assertNull(driver.notes)
        }

        @Test
        fun `client with all nullable fields`() {
            val id = repo.upsertClient(id = null, name = "C", contact = null, notes = null)
            val client = repo.listClients().first { it.id == id }
            assertNull(client.contact)
            assertNull(client.notes)
        }
    }

    // ── Transport Validation ──────────────────────────────────────────

    @Nested
    inner class TransportValidation {
        @Test
        fun `start transport fails with empty plate`() {
            val driverId = repo.upsertDriver(id = null, name = "D", phone = null, notes = null)
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 10.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
                acquisitionStorageLocation = PoleStorageLocation.EN_PROVEEDOR,
            )
            val result = repo.startProviderTransport(
                driverId = driverId, vehiclePlate = "  ",
                freightCost = 100.0, gruaCost = 0.0,
                departedAtEpochMs = System.currentTimeMillis(),
                expectedArrivalEpochMs = null,
                productIds = listOf(prodId), notes = null,
            )
            assertTrue(result is InventoryRepository.TransportRunResult.Err)
        }

        @Test
        fun `start transport fails with negative costs`() {
            val driverId = repo.upsertDriver(id = null, name = "D", phone = null, notes = null)
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 10.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
                acquisitionStorageLocation = PoleStorageLocation.EN_PROVEEDOR,
            )
            val result = repo.startProviderTransport(
                driverId = driverId, vehiclePlate = "AA-11",
                freightCost = -100.0, gruaCost = 0.0,
                departedAtEpochMs = System.currentTimeMillis(),
                expectedArrivalEpochMs = null,
                productIds = listOf(prodId), notes = null,
            )
            assertTrue(result is InventoryRepository.TransportRunResult.Err)
        }

        @Test
        fun `start transport fails with empty product list`() {
            val driverId = repo.upsertDriver(id = null, name = "D", phone = null, notes = null)
            val result = repo.startProviderTransport(
                driverId = driverId, vehiclePlate = "AA-11",
                freightCost = 100.0, gruaCost = 0.0,
                departedAtEpochMs = System.currentTimeMillis(),
                expectedArrivalEpochMs = null,
                productIds = emptyList(), notes = null,
            )
            assertTrue(result is InventoryRepository.TransportRunResult.Err)
        }

        @Test
        fun `start transport fails with invalid driver`() {
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 10.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
                acquisitionStorageLocation = PoleStorageLocation.EN_PROVEEDOR,
            )
            val result = repo.startProviderTransport(
                driverId = 99999, vehiclePlate = "AA-11",
                freightCost = 100.0, gruaCost = 0.0,
                departedAtEpochMs = System.currentTimeMillis(),
                expectedArrivalEpochMs = null,
                productIds = listOf(prodId), notes = null,
            )
            assertTrue(result is InventoryRepository.TransportRunResult.Err)
        }

        @Test
        fun `start transport fails with non-EN_PROVEEDOR product`() {
            val driverId = repo.upsertDriver(id = null, name = "D", phone = null, notes = null)
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 10.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
                acquisitionStorageLocation = PoleStorageLocation.FABRICA,
            )
            val result = repo.startProviderTransport(
                driverId = driverId, vehiclePlate = "AA-11",
                freightCost = 100.0, gruaCost = 0.0,
                departedAtEpochMs = System.currentTimeMillis(),
                expectedArrivalEpochMs = null,
                productIds = listOf(prodId), notes = null,
            )
            assertTrue(result is InventoryRepository.TransportRunResult.Err)
        }

        @Test
        fun `start transport fails with failed product`() {
            val driverId = repo.upsertDriver(id = null, name = "D", phone = null, notes = null)
            val prodId = TestDataBuilder.createProduct(
                name = "P", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 10.0,
                acquisitionStorageLocation = PoleStorageLocation.EN_PROVEEDOR,
                isFailed = true, failedAtStage = ProductStage.CRUDO,
            )
            val result = repo.startProviderTransport(
                driverId = driverId, vehiclePlate = "AA-11",
                freightCost = 100.0, gruaCost = 0.0,
                departedAtEpochMs = System.currentTimeMillis(),
                expectedArrivalEpochMs = null,
                productIds = listOf(prodId), notes = null,
            )
            assertTrue(result is InventoryRepository.TransportRunResult.Err)
        }
    }

    // ── Transformation Edge Cases ─────────────────────────────────────

    @Nested
    inner class TransformationEdgeCases {
        @Test
        fun `create transformation fails with empty inputs`() {
            val result = repo.createTransformation(
                fromStage = ProductStage.CRUDO,
                inputs = emptyList(),
                successCount = 10.0,
                failedCount = 0.0,
                durationMinutes = 60,
                processedAtEpochMs = System.currentTimeMillis(),
                notes = null,
                resourceUses = emptyList(),
            )
            assertTrue(result is InventoryRepository.TransformationResult.Err)
        }

        @Test
        fun `create transformation fails with mismatched counts`() {
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 10.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
                acquisitionStorageLocation = PoleStorageLocation.FABRICA,
            )
            val result = repo.createTransformation(
                fromStage = ProductStage.CRUDO,
                inputs = listOf(InventoryRepository.SourceDraft(prodId, 10.0)),
                successCount = 8.0,
                failedCount = 1.0, // 8+1=9 ≠ 10
                durationMinutes = 60,
                processedAtEpochMs = System.currentTimeMillis(),
                notes = null,
                resourceUses = emptyList(),
            )
            assertTrue(result is InventoryRepository.TransformationResult.Err)
        }

        @Test
        fun `create transformation fails with zero quantity input`() {
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 10.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
                acquisitionStorageLocation = PoleStorageLocation.FABRICA,
            )
            val result = repo.createTransformation(
                fromStage = ProductStage.CRUDO,
                inputs = listOf(InventoryRepository.SourceDraft(prodId, 0.0)),
                successCount = 0.0,
                failedCount = 0.0,
                durationMinutes = 60,
                processedAtEpochMs = System.currentTimeMillis(),
                notes = null,
                resourceUses = emptyList(),
            )
            assertTrue(result is InventoryRepository.TransformationResult.Err)
        }

        @Test
        fun `create transformation fails with negative counts`() {
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 10.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
                acquisitionStorageLocation = PoleStorageLocation.FABRICA,
            )
            val result = repo.createTransformation(
                fromStage = ProductStage.CRUDO,
                inputs = listOf(InventoryRepository.SourceDraft(prodId, 10.0)),
                successCount = -1.0,
                failedCount = 11.0,
                durationMinutes = 60,
                processedAtEpochMs = System.currentTimeMillis(),
                notes = null,
                resourceUses = emptyList(),
            )
            assertTrue(result is InventoryRepository.TransformationResult.Err)
        }

        @Test
        fun `create transformation fails with insufficient stock`() {
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 5.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
                acquisitionStorageLocation = PoleStorageLocation.FABRICA,
            )
            val result = repo.createTransformation(
                fromStage = ProductStage.CRUDO,
                inputs = listOf(InventoryRepository.SourceDraft(prodId, 10.0)),
                successCount = 10.0,
                failedCount = 0.0,
                durationMinutes = 60,
                processedAtEpochMs = System.currentTimeMillis(),
                notes = null,
                resourceUses = emptyList(),
            )
            assertTrue(result is InventoryRepository.TransformationResult.Err)
        }

        @Test
        fun `create transformation fails when product not in FABRICA`() {
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 10.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
                acquisitionStorageLocation = PoleStorageLocation.EN_PROVEEDOR,
            )
            val result = repo.createTransformation(
                fromStage = ProductStage.CRUDO,
                inputs = listOf(InventoryRepository.SourceDraft(prodId, 10.0)),
                successCount = 10.0,
                failedCount = 0.0,
                durationMinutes = 60,
                processedAtEpochMs = System.currentTimeMillis(),
                notes = null,
                resourceUses = emptyList(),
            )
            assertTrue(result is InventoryRepository.TransformationResult.Err)
        }

        @Test
        fun `create transformation fails when product is failed`() {
            val prodId = TestDataBuilder.createProduct(
                name = "P", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 10.0,
                acquisitionStorageLocation = PoleStorageLocation.FABRICA,
                isFailed = true, failedAtStage = ProductStage.CRUDO,
            )
            val result = repo.createTransformation(
                fromStage = ProductStage.CRUDO,
                inputs = listOf(InventoryRepository.SourceDraft(prodId, 10.0)),
                successCount = 10.0,
                failedCount = 0.0,
                durationMinutes = 60,
                processedAtEpochMs = System.currentTimeMillis(),
                notes = null,
                resourceUses = emptyList(),
            )
            assertTrue(result is InventoryRepository.TransformationResult.Err)
        }

        @Test
        fun `create transformation fails when product in wrong stage`() {
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.TERMINADO, quantity = 10.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
                acquisitionStorageLocation = PoleStorageLocation.FABRICA,
            )
            val result = repo.createTransformation(
                fromStage = ProductStage.CRUDO,
                inputs = listOf(InventoryRepository.SourceDraft(prodId, 10.0)),
                successCount = 10.0,
                failedCount = 0.0,
                durationMinutes = 60,
                processedAtEpochMs = System.currentTimeMillis(),
                notes = null,
                resourceUses = emptyList(),
            )
            assertTrue(result is InventoryRepository.TransformationResult.Err)
        }

        @Test
        fun `successful transformation produces output lot`() {
            val prodId = repo.upsertProduct(
                id = null, name = "Pino 8m", productLine = "Distribución",
                stage = ProductStage.CRUDO, quantity = 50.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = 95_000.0, failedSalePrice = 35_000.0,
                acquisitionCostPerPole = 25_000.0,
                acquisitionStorageLocation = PoleStorageLocation.FABRICA,
            )
            val result = repo.createTransformation(
                fromStage = ProductStage.CRUDO,
                inputs = listOf(InventoryRepository.SourceDraft(prodId, 45.0)),
                successCount = 42.0,
                failedCount = 3.0,
                durationMinutes = 480,
                processedAtEpochMs = System.currentTimeMillis(),
                notes = "Test transformation",
                resourceUses = emptyList(),
            )
            assertTrue(result is InventoryRepository.TransformationResult.Ok)

            // Source lot reduced
            val source = repo.getProduct(prodId)
            assertEquals(5.0, source!!.quantity)

            // New DESCORTEZADO lot created
            val descort = repo.listProducts(ProductStage.DESCORTEZADO)
            assertEquals(1, descort.size)
            assertEquals(42.0, descort[0].quantity)

            // Failed lot at CRUDO
            val failed = repo.listProducts(ProductStage.CRUDO).filter { it.isFailed }
            assertEquals(1, failed.size)
            assertEquals(3.0, failed[0].quantity)
        }

        @Test
        fun `transformation with all success and no failures`() {
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 30.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
                acquisitionStorageLocation = PoleStorageLocation.FABRICA,
            )
            val result = repo.createTransformation(
                fromStage = ProductStage.CRUDO,
                inputs = listOf(InventoryRepository.SourceDraft(prodId, 30.0)),
                successCount = 30.0,
                failedCount = 0.0,
                durationMinutes = 120,
                processedAtEpochMs = System.currentTimeMillis(),
                notes = null,
                resourceUses = emptyList(),
            )
            assertTrue(result is InventoryRepository.TransformationResult.Ok)

            val descort = repo.listProducts(ProductStage.DESCORTEZADO)
            assertEquals(1, descort.size)
            assertEquals(30.0, descort[0].quantity)
            assertFalse(descort[0].isFailed)
        }
    }

    // ── Resource Stock Lots ───────────────────────────────────────────

    @Nested
    inner class ResourceStockLots {
        @Test
        fun `create and list resource stock lots`() {
            val resId = repo.upsertResource(id = null, name = "Agua", unit = "L", costPerUnit = 5.0)
            repo.upsertResourceStockLot(
                id = null, resourceId = resId, quantity = 1000.0,
                acquisitionPricePerUnit = 4.5, expirationDate = null, notes = null,
            )
            repo.upsertResourceStockLot(
                id = null, resourceId = resId, quantity = 500.0,
                acquisitionPricePerUnit = 5.5, expirationDate = null, notes = null,
            )
            val lots = repo.listResourceStockLots()
            assertEquals(2, lots.size)
        }

        @Test
        fun `resource stock total value estimate is correct`() {
            val resId = repo.upsertResource(id = null, name = "Agua", unit = "L", costPerUnit = 5.0)
            repo.upsertResourceStockLot(
                id = null, resourceId = resId, quantity = 1000.0,
                acquisitionPricePerUnit = 4.0, expirationDate = null, notes = null,
            )
            repo.upsertResourceStockLot(
                id = null, resourceId = resId, quantity = 500.0,
                acquisitionPricePerUnit = 6.0, expirationDate = null, notes = null,
            )
            val total = repo.resourceStockTotalValueEstimate()
            assertEquals(7_000.0, total) // 1000*4 + 500*6
        }

        @Test
        fun `delete resource stock lot`() {
            val resId = repo.upsertResource(id = null, name = "Agua", unit = "L", costPerUnit = 5.0)
            val lotId = repo.upsertResourceStockLot(
                id = null, resourceId = resId, quantity = 1000.0,
                acquisitionPricePerUnit = 4.0, expirationDate = null, notes = null,
            )
            repo.deleteResourceStockLot(lotId)
            assertTrue(repo.listResourceStockLots().isEmpty())
        }
    }

    // ── Stage Resource Templates ──────────────────────────────────────

    @Nested
    inner class StageResourceTemplates {
        @Test
        fun `create and list stage resource templates`() {
            val resId = repo.upsertResource(id = null, name = "Agua", unit = "L", costPerUnit = 5.0)
            repo.upsertStageResourceTemplate(
                id = null, fromStage = ProductStage.CRUDO,
                resourceId = resId, amountPerPole = 15.0,
                notes = "Lavado", displayOrder = 1,
            )
            val templates = repo.listStageResourceTemplates(ProductStage.CRUDO)
            assertEquals(1, templates.size)
            assertEquals(15.0, templates[0].amountPerPole)
        }

        @Test
        fun `suggest resource uses from recipe`() {
            val resId = repo.upsertResource(id = null, name = "Agua", unit = "L", costPerUnit = 5.0)
            repo.upsertStageResourceTemplate(
                id = null, fromStage = ProductStage.CRUDO,
                resourceId = resId, amountPerPole = 15.0,
                notes = null, displayOrder = 1,
            )
            val uses = repo.suggestResourceUsesFromRecipe(ProductStage.CRUDO, poleCount = 20.0)
            assertEquals(1, uses.size)
            assertEquals(300.0, uses[0].amount) // 15 * 20
        }

        @Test
        fun `suggest resource uses with zero poles returns empty`() {
            val uses = repo.suggestResourceUsesFromRecipe(ProductStage.CRUDO, poleCount = 0.0)
            assertTrue(uses.isEmpty())
        }
    }
}
