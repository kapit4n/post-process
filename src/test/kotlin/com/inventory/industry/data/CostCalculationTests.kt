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

class CostCalculationTests {

    private lateinit var repo: InventoryRepository

    @BeforeEach
    fun setup() {
        TestDatabaseHelper.withCleanDb {}
        repo = InventoryRepository()
    }

    // ── Transport Costs ───────────────────────────────────────────────

    @Nested
    inner class TransportCosts {
        @Test
        fun `acquisition transport total is zero when no costs`() {
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 50.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = 25_000.0,
            )
            val total = repo.acquisitionTransportTotalForProduct(prodId)
            assertEquals(0.0, total)
        }

        @Test
        fun `acquisition transport total sums all lines`() {
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 50.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = 25_000.0,
            )
            repo.syncAcquisitionTransportCosts(
                productId = prodId,
                location = PoleStorageLocation.EN_PROVEEDOR,
                lines = listOf(
                    AcquisitionTransportLineDraft(label = "Flete", lineCost = 500_000.0),
                    AcquisitionTransportLineDraft(label = "Carga", lineCost = 100_000.0),
                ),
            )
            val total = repo.acquisitionTransportTotalForProduct(prodId)
            assertEquals(600_000.0, total)
        }

        @Test
        fun `list acquisition transport returns all lines`() {
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 50.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
            )
            repo.syncAcquisitionTransportCosts(
                productId = prodId,
                location = PoleStorageLocation.EN_PROVEEDOR,
                lines = listOf(
                    AcquisitionTransportLineDraft(label = "Flete", lineCost = 500_000.0),
                    AcquisitionTransportLineDraft(label = "Grua", lineCost = 80_000.0),
                ),
            )
            val lines = repo.listAcquisitionTransportForProduct(prodId)
            assertEquals(2, lines.size)
            assertEquals("Flete", lines[0].label)
            assertEquals(500_000.0, lines[0].lineCost)
        }

        @Test
        fun `sync transport costs replaces existing lines`() {
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 50.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
            )
            repo.syncAcquisitionTransportCosts(
                productId = prodId,
                location = PoleStorageLocation.EN_PROVEEDOR,
                lines = listOf(AcquisitionTransportLineDraft(label = "Old", lineCost = 100.0)),
            )
            repo.syncAcquisitionTransportCosts(
                productId = prodId,
                location = PoleStorageLocation.EN_PROVEEDOR,
                lines = listOf(AcquisitionTransportLineDraft(label = "New", lineCost = 200.0)),
            )
            val lines = repo.listAcquisitionTransportForProduct(prodId)
            assertEquals(1, lines.size)
            assertEquals("New", lines[0].label)
            assertEquals(200.0, lines[0].lineCost)
        }

        @Test
        fun `sync transport costs clears lines when FABRICA`() {
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 50.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
            )
            repo.syncAcquisitionTransportCosts(
                productId = prodId,
                location = PoleStorageLocation.EN_PROVEEDOR,
                lines = listOf(AcquisitionTransportLineDraft(label = "Flete", lineCost = 100.0)),
            )
            repo.syncAcquisitionTransportCosts(
                productId = prodId,
                location = PoleStorageLocation.FABRICA,
                lines = emptyList(),
            )
            val lines = repo.listAcquisitionTransportForProduct(prodId)
            assertTrue(lines.isEmpty())
        }

        @Test
        fun `transport run completes and prorates costs`() {
            val driverId = repo.upsertDriver(id = null, name = "D", phone = null, notes = null)
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 100.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = 25_000.0,
                acquisitionStorageLocation = PoleStorageLocation.EN_PROVEEDOR,
            )
            val result = repo.startProviderTransport(
                driverId = driverId, vehiclePlate = "AA-11",
                freightCost = 1_000_000.0, gruaCost = 200_000.0,
                departedAtEpochMs = System.currentTimeMillis(),
                expectedArrivalEpochMs = null,
                productIds = listOf(prodId), notes = null,
            )
            assertTrue(result is InventoryRepository.TransportRunResult.Ok)
            val runId = (result as InventoryRepository.TransportRunResult.Ok).runId

            val completeResult = repo.completeProviderTransport(
                runId = runId,
                arrivedAtEpochMs = System.currentTimeMillis(),
            )
            assertTrue(completeResult is InventoryRepository.TransportRunResult.Ok)

            val total = repo.acquisitionTransportTotalForProduct(prodId)
            assertEquals(1_200_000.0, total)

            val prod = repo.getProduct(prodId)!!
            assertEquals(PoleStorageLocation.FABRICA, prod.acquisitionStorageLocation)
        }

        @Test
        fun `transport run cancel returns products to EN_PROVEEDOR`() {
            val driverId = repo.upsertDriver(id = null, name = "D", phone = null, notes = null)
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 100.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
                acquisitionStorageLocation = PoleStorageLocation.EN_PROVEEDOR,
            )
            val result = repo.startProviderTransport(
                driverId = driverId, vehiclePlate = "AA-11",
                freightCost = 500_000.0, gruaCost = 0.0,
                departedAtEpochMs = System.currentTimeMillis(),
                expectedArrivalEpochMs = null,
                productIds = listOf(prodId), notes = null,
            )
            val runId = (result as InventoryRepository.TransportRunResult.Ok).runId
            repo.cancelProviderTransport(runId)

            val prod = repo.getProduct(prodId)!!
            assertEquals(PoleStorageLocation.EN_PROVEEDOR, prod.acquisitionStorageLocation)
        }
    }

    // ── Process Costs ─────────────────────────────────────────────────

    @Nested
    inner class ProcessCosts {
        @Test
        fun `processing cost total is zero when no costs`() {
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 50.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
            )
            assertEquals(0.0, repo.processingCostTotalForProduct(prodId))
        }

        @Test
        fun `processing cost total sums all lines`() {
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.DESCORTEZADO, quantity = 50.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
            )
            val resId = repo.upsertResource(id = null, name = "Agua", unit = "L", costPerUnit = 5.0)
            repo.createProcessCostForTesting(
                productId = prodId, fromStage = ProductStage.CRUDO,
                toStage = ProductStage.DESCORTEZADO, resourceId = resId,
                amountUsed = 100.0, lineCost = 500.0,
            )
            repo.createProcessCostForTesting(
                productId = prodId, fromStage = ProductStage.CRUDO,
                toStage = ProductStage.DESCORTEZADO, resourceId = resId,
                amountUsed = 200.0, lineCost = 1_000.0,
            )
            assertEquals(1_500.0, repo.processingCostTotalForProduct(prodId))
        }

        @Test
        fun `list costs for product returns all lines`() {
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.DESCORTEZADO, quantity = 50.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
            )
            repo.createProcessCostForTesting(
                productId = prodId, fromStage = ProductStage.CRUDO,
                toStage = ProductStage.DESCORTEZADO, resourceId = null,
                amountUsed = null, lineCost = 100.0,
            )
            val costs = repo.listCostsForProduct(prodId)
            assertEquals(1, costs.size)
            assertEquals(100.0, costs[0].lineCost)
        }
    }

    // ── Sale Cost Preview ─────────────────────────────────────────────

    @Nested
    inner class SaleCostPreview {
        @Test
        fun `sale cost preview calculates correctly`() {
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.TERMINADO, quantity = 100.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = 95_000.0, failedSalePrice = 35_000.0,
                acquisitionCostPerPole = 25_000.0,
            )
            repo.syncAcquisitionTransportCosts(
                productId = prodId,
                location = PoleStorageLocation.EN_PROVEEDOR,
                lines = listOf(AcquisitionTransportLineDraft(label = "Flete", lineCost = 500_000.0)),
            )
            val resId = repo.upsertResource(id = null, name = "Agua", unit = "L", costPerUnit = 5.0)
            repo.createProcessCostForTesting(
                productId = prodId, fromStage = ProductStage.TRATADO,
                toStage = ProductStage.TERMINADO, resourceId = resId,
                amountUsed = 100.0, lineCost = 500.0,
            )

            val preview = repo.saleCostPreview(
                productId = prodId, quantitySold = 10.0, marginPercent = 35.0,
            )
            assertNotNull(preview)
            assertEquals(100.0, preview.quantityAvailable)
            assertEquals(25_000.0, preview.acquisitionMaterialPerPole)
            assertEquals(5_000.0, preview.acquisitionTransportPerPole) // 500K / 100
            assertEquals(30_000.0, preview.landedAcquisitionPerPole) // 25K + 5K
            assertEquals(500.0, preview.processingCostTotalOnLot)
            assertEquals(5.0, preview.processingCostPerPole) // 500 / 100
            assertEquals(30_005.0, preview.unitCostBasis) // 30K + 5
            // suggestedUnitPrice = 30_005 * 1.35 = 40_506.75
            assertEquals(30_005.0 * 1.35, preview.suggestedUnitPrice, 0.01)
        }

        @Test
        fun `sale cost preview returns null for invalid product`() {
            assertNull(repo.saleCostPreview(productId = 99999, quantitySold = 10.0, marginPercent = 20.0))
        }
    }

    // ── Accounting Overview ───────────────────────────────────────────

    @Nested
    inner class AccountingOverview {
        @Test
        fun `empty inventory has zero accounting overview`() {
            val overview = repo.accountingCostOverview()
            assertEquals(0.0, overview.totalProcessingCostAllTime)
            assertEquals(0.0, overview.processingCostAttributedToOpenStock)
            assertEquals(0.0, overview.inventoryAcquisitionCostTotal)
            assertEquals(0.0, overview.soldAcquisitionCostTotal)
            assertEquals(0.0, overview.soldProcessingCostTotal)
        }

        @Test
        fun `accounting overview reflects inventory acquisition cost`() {
            repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 100.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = 25_000.0,
            )
            val overview = repo.accountingCostOverview()
            assertEquals(2_500_000.0, overview.inventoryAcquisitionCostTotal)
        }

        @Test
        fun `accounting overview includes process costs`() {
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.DESCORTEZADO, quantity = 50.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = null, failedSalePrice = null,
                acquisitionCostPerPole = null,
            )
            repo.createProcessCostForTesting(
                productId = prodId, fromStage = ProductStage.CRUDO,
                toStage = ProductStage.DESCORTEZADO, resourceId = null,
                amountUsed = null, lineCost = 10_000.0,
            )
            val overview = repo.accountingCostOverview()
            assertEquals(10_000.0, overview.totalProcessingCostAllTime)
            assertEquals(10_000.0, overview.processingCostAttributedToOpenStock)
        }
    }

    // ── Sales Aggregation ─────────────────────────────────────────────

    @Nested
    inner class SalesAggregation {
        @Test
        fun `monthly aggregation returns 12 buckets`() {
            val buckets = repo.salesAggregatedMonthly(year = 2026)
            assertEquals(12, buckets.size)
        }

        @Test
        fun `yearly aggregation returns buckets by year`() {
            val clientId = repo.upsertClient(id = null, name = "C", contact = null, notes = null)
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.TERMINADO, quantity = 10.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = 100_000.0, failedSalePrice = null,
                acquisitionCostPerPole = null,
            )
            repo.recordSale(
                productId = prodId, clientId = clientId, quantitySold = 5.0,
                totalAmount = 500_000.0, soldAtEpochMs = System.currentTimeMillis(),
                notes = null,
            )
            val buckets = repo.salesAggregatedYearly()
            assertTrue(buckets.isNotEmpty())
        }
    }

    // ── Sales Recording ───────────────────────────────────────────────

    @Nested
    inner class SalesRecording {
        @Test
        fun `record sale reduces product quantity`() {
            val clientId = repo.upsertClient(id = null, name = "C", contact = null, notes = null)
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.TERMINADO, quantity = 50.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = 100_000.0, failedSalePrice = null,
                acquisitionCostPerPole = 25_000.0,
            )
            val result = repo.recordSale(
                productId = prodId, clientId = clientId, quantitySold = 10.0,
                totalAmount = 1_000_000.0, soldAtEpochMs = System.currentTimeMillis(),
                notes = null, marginPercentForEstimate = 35.0,
            )
            assertTrue(result is InventoryRepository.SaleRecordingResult.Ok)
            val prod = repo.getProduct(prodId)!!
            assertEquals(40.0, prod.quantity)
        }

        @Test
        fun `record sale deletes product when quantity reaches zero`() {
            val clientId = repo.upsertClient(id = null, name = "C", contact = null, notes = null)
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.TERMINADO, quantity = 10.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = 100_000.0, failedSalePrice = null,
                acquisitionCostPerPole = null,
            )
            repo.recordSale(
                productId = prodId, clientId = clientId, quantitySold = 10.0,
                totalAmount = 1_000_000.0, soldAtEpochMs = System.currentTimeMillis(),
                notes = null,
            )
            assertNull(repo.getProduct(prodId))
        }

        @Test
        fun `record sale with snapshot contains correct data`() {
            val provId = repo.upsertPoleProvider(id = null, name = "Prov", contact = null, notes = null)
            val clientId = repo.upsertClient(id = null, name = "C", contact = null, notes = null)
            val prodId = repo.upsertProduct(
                id = null, name = "Poste Test", productLine = "Línea Test",
                stage = ProductStage.TERMINADO, quantity = 20.0, notes = null,
                catalogProductId = null, providerId = provId,
                standardSalePrice = 100_000.0, failedSalePrice = null,
                acquisitionCostPerPole = 25_000.0,
            )
            repo.recordSale(
                productId = prodId, clientId = clientId, quantitySold = 5.0,
                totalAmount = 500_000.0, soldAtEpochMs = System.currentTimeMillis(),
                notes = "Test sale", marginPercentForEstimate = 35.0,
            )
            val sales = repo.listSales()
            assertEquals(1, sales.size)
            val sale = sales[0]
            assertEquals("Poste Test", sale.snapshotProductName)
            assertEquals("Línea Test", sale.snapshotProductLine)
            assertEquals(ProductStage.TERMINADO, sale.snapshotStage)
            assertEquals(false, sale.snapshotWasFailed)
            assertEquals("Prov", sale.snapshotProviderName)
            assertEquals(5.0, sale.quantitySold)
            assertEquals(500_000.0, sale.totalAmount)
            assertEquals(100_000.0, sale.unitPrice)
            assertEquals(35.0, sale.snapshotMarginPercent)
        }

        @Test
        fun `record sale fails with invalid client`() {
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.TERMINADO, quantity = 10.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = 100.0, failedSalePrice = null,
                acquisitionCostPerPole = null,
            )
            val result = repo.recordSale(
                productId = prodId, clientId = 99999, quantitySold = 5.0,
                totalAmount = 500.0, soldAtEpochMs = System.currentTimeMillis(),
                notes = null,
            )
            assertTrue(result is InventoryRepository.SaleRecordingResult.Err)
        }

        @Test
        fun `record sale fails with invalid product`() {
            val clientId = repo.upsertClient(id = null, name = "C", contact = null, notes = null)
            val result = repo.recordSale(
                productId = 99999, clientId = clientId, quantitySold = 5.0,
                totalAmount = 500.0, soldAtEpochMs = System.currentTimeMillis(),
                notes = null,
            )
            assertTrue(result is InventoryRepository.SaleRecordingResult.Err)
        }

        @Test
        fun `record sale fails when product not sellable`() {
            val clientId = repo.upsertClient(id = null, name = "C", contact = null, notes = null)
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 10.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = 100.0, failedSalePrice = null,
                acquisitionCostPerPole = null,
            )
            val result = repo.recordSale(
                productId = prodId, clientId = clientId, quantitySold = 5.0,
                totalAmount = 500.0, soldAtEpochMs = System.currentTimeMillis(),
                notes = null,
            )
            assertTrue(result is InventoryRepository.SaleRecordingResult.Err)
        }

        @Test
        fun `record sale fails with negative amount`() {
            val clientId = repo.upsertClient(id = null, name = "C", contact = null, notes = null)
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.TERMINADO, quantity = 10.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = 100.0, failedSalePrice = null,
                acquisitionCostPerPole = null,
            )
            val result = repo.recordSale(
                productId = prodId, clientId = clientId, quantitySold = 5.0,
                totalAmount = -100.0, soldAtEpochMs = System.currentTimeMillis(),
                notes = null,
            )
            assertTrue(result is InventoryRepository.SaleRecordingResult.Err)
        }

        @Test
        fun `record sale fails with quantity exceeding available`() {
            val clientId = repo.upsertClient(id = null, name = "C", contact = null, notes = null)
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.TERMINADO, quantity = 10.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = 100.0, failedSalePrice = null,
                acquisitionCostPerPole = null,
            )
            val result = repo.recordSale(
                productId = prodId, clientId = clientId, quantitySold = 20.0,
                totalAmount = 2_000.0, soldAtEpochMs = System.currentTimeMillis(),
                notes = null,
            )
            assertTrue(result is InventoryRepository.SaleRecordingResult.Err)
        }

        @Test
        fun `record sale fails with zero quantity`() {
            val clientId = repo.upsertClient(id = null, name = "C", contact = null, notes = null)
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.TERMINADO, quantity = 10.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = 100.0, failedSalePrice = null,
                acquisitionCostPerPole = null,
            )
            val result = repo.recordSale(
                productId = prodId, clientId = clientId, quantitySold = 0.0,
                totalAmount = 0.0, soldAtEpochMs = System.currentTimeMillis(),
                notes = null,
            )
            assertTrue(result is InventoryRepository.SaleRecordingResult.Err)
        }

        @Test
        fun `failed product can be sold at salvage price`() {
            val clientId = repo.upsertClient(id = null, name = "C", contact = null, notes = null)
            val prodId = TestDataBuilder.createProduct(
                name = "P", productLine = "L",
                stage = ProductStage.CRUDO, quantity = 5.0,
                standardSalePrice = 100_000.0, failedSalePrice = 30_000.0,
                acquisitionCostPerPole = 25_000.0,
                isFailed = true, failedAtStage = ProductStage.CRUDO,
            )
            val result = repo.recordSale(
                productId = prodId, clientId = clientId, quantitySold = 3.0,
                totalAmount = 90_000.0, soldAtEpochMs = System.currentTimeMillis(),
                notes = "Salvage sale",
            )
            assertTrue(result is InventoryRepository.SaleRecordingResult.Ok)
        }

        @Test
        fun `list sales respects limit`() {
            val clientId = repo.upsertClient(id = null, name = "C", contact = null, notes = null)
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.TERMINADO, quantity = 100.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = 100.0, failedSalePrice = null,
                acquisitionCostPerPole = null,
            )
            repeat(5) { i ->
                repo.recordSale(
                    productId = prodId, clientId = clientId, quantitySold = 1.0,
                    totalAmount = 100.0, soldAtEpochMs = System.currentTimeMillis() + i,
                    notes = null,
                )
            }
            val sales = repo.listSales(limit = 3)
            assertEquals(3, sales.size)
        }

        @Test
        fun `sales in range filters correctly`() {
            val clientId = repo.upsertClient(id = null, name = "C", contact = null, notes = null)
            val prodId = repo.upsertProduct(
                id = null, name = "P", productLine = "L",
                stage = ProductStage.TERMINADO, quantity = 100.0, notes = null,
                catalogProductId = null, providerId = null,
                standardSalePrice = 100.0, failedSalePrice = null,
                acquisitionCostPerPole = null,
            )
            val now = System.currentTimeMillis()
            repo.recordSale(
                productId = prodId, clientId = clientId, quantitySold = 1.0,
                totalAmount = 100.0, soldAtEpochMs = now - 10_000, notes = null,
            )
            repo.recordSale(
                productId = prodId, clientId = clientId, quantitySold = 1.0,
                totalAmount = 100.0, soldAtEpochMs = now, notes = null,
            )
            repo.recordSale(
                productId = prodId, clientId = clientId, quantitySold = 1.0,
                totalAmount = 100.0, soldAtEpochMs = now + 10_000, notes = null,
            )

            val range = repo.listSalesInRange(fromEpochMs = now - 5_000, toEpochMsExclusive = now + 5_000)
            assertEquals(1, range.size)
        }
    }

    // Helper to insert process costs directly (bypassing transformation logic)
    private fun InventoryRepository.createProcessCostForTesting(
        productId: Int,
        fromStage: ProductStage,
        toStage: ProductStage,
        resourceId: Int?,
        amountUsed: Double?,
        lineCost: Double,
    ) {
        TestDataBuilder.createProcessCost(
            productId = productId,
            fromStage = fromStage,
            toStage = toStage,
            resourceId = resourceId,
            amountUsed = amountUsed,
            lineCost = lineCost,
        )
    }
}
