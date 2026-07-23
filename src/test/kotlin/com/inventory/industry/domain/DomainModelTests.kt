package com.inventory.industry.domain

import com.inventory.industry.data.Product
import com.inventory.industry.data.isSellable
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DomainModelTests {

    // ── ProductStage ──────────────────────────────────────────────────

    @Nested
    inner class ProductStageTests {
        @Test
        fun `fromDb returns correct stage for valid names`() {
            assertEquals(ProductStage.CRUDO, ProductStage.fromDb("CRUDO"))
            assertEquals(ProductStage.DESCORTEZADO, ProductStage.fromDb("DESCORTEZADO"))
            assertEquals(ProductStage.TRATADO, ProductStage.fromDb("TRATADO"))
            assertEquals(ProductStage.TERMINADO, ProductStage.fromDb("TERMINADO"))
        }

        @Test
        fun `fromDb is case insensitive`() {
            assertEquals(ProductStage.CRUDO, ProductStage.fromDb("crudo"))
            assertEquals(ProductStage.DESCORTEZADO, ProductStage.fromDb("Descortezado"))
        }

        @Test
        fun `fromDb handles legacy codes`() {
            assertEquals(ProductStage.CRUDO, ProductStage.fromDb("X"))
            assertEquals(ProductStage.DESCORTEZADO, ProductStage.fromDb("Y"))
            assertEquals(ProductStage.TRATADO, ProductStage.fromDb("Y2"))
            assertEquals(ProductStage.TERMINADO, ProductStage.fromDb("Z"))
        }

        @Test
        fun `fromDb throws on unknown value`() {
            try {
                ProductStage.fromDb("UNKNOWN")
                assertTrue(false, "Should have thrown")
            } catch (e: IllegalStateException) {
                assertTrue(e.message!!.contains("Etapa desconocida"))
            }
        }

        @Test
        fun `next returns correct successor`() {
            assertEquals(ProductStage.DESCORTEZADO, ProductStage.CRUDO.next())
            assertEquals(ProductStage.TRATADO, ProductStage.DESCORTEZADO.next())
            assertEquals(ProductStage.TERMINADO, ProductStage.TRATADO.next())
            assertNull(ProductStage.TERMINADO.next())
        }

        @Test
        fun `shortCode returns correct values`() {
            assertEquals("Crudo", ProductStage.CRUDO.shortCode)
            assertEquals("Descort.", ProductStage.DESCORTEZADO.shortCode)
            assertEquals("Tratado", ProductStage.TRATADO.shortCode)
            assertEquals("Terminado", ProductStage.TERMINADO.shortCode)
        }

        @Test
        fun `title returns correct values`() {
            assertEquals("Crudo — Recibido del proveedor", ProductStage.CRUDO.title)
            assertEquals("Descortezado y Secado", ProductStage.DESCORTEZADO.title)
            assertEquals("Tratado Químicamente", ProductStage.TRATADO.title)
            assertEquals("Terminado — Listo para venta", ProductStage.TERMINADO.title)
        }
    }

    // ── PoleStorageLocation ───────────────────────────────────────────

    @Nested
    inner class PoleStorageLocationTests {
        @Test
        fun `fromDb returns correct location for valid names`() {
            assertEquals(PoleStorageLocation.FABRICA, PoleStorageLocation.fromDb("FABRICA"))
            assertEquals(PoleStorageLocation.EN_PROVEEDOR, PoleStorageLocation.fromDb("EN_PROVEEDOR"))
            assertEquals(PoleStorageLocation.EN_TRANSITO, PoleStorageLocation.fromDb("EN_TRANSITO"))
        }

        @Test
        fun `fromDb is case insensitive`() {
            assertEquals(PoleStorageLocation.FABRICA, PoleStorageLocation.fromDb("fabrica"))
            assertEquals(PoleStorageLocation.EN_PROVEEDOR, PoleStorageLocation.fromDb("en_proveedor"))
        }

        @Test
        fun `fromDb defaults to FABRICA for unknown`() {
            assertEquals(PoleStorageLocation.FABRICA, PoleStorageLocation.fromDb("UNKNOWN"))
        }

        @Test
        fun `shortLabel returns correct values`() {
            assertEquals("Fábrica", PoleStorageLocation.FABRICA.shortLabel)
            assertEquals("Proveedor", PoleStorageLocation.EN_PROVEEDOR.shortLabel)
            assertEquals("En traslado", PoleStorageLocation.EN_TRANSITO.shortLabel)
        }
    }

    // ── Product Extension Functions ───────────────────────────────────

    @Nested
    inner class ProductExtensions {
        private fun testProduct(
            stage: ProductStage = ProductStage.TERMINADO,
            isFailed: Boolean = false,
            standardSalePrice: Double? = 100_000.0,
            failedSalePrice: Double? = 30_000.0,
        ) = Product(
            id = 1, name = "Test", productLine = "L",
            stage = stage, quantity = 10.0, notes = null,
            createdAtEpochMs = 0L, catalogProductId = null,
            providerId = null, providerName = null,
            isFailed = isFailed, failedAtStage = null,
            standardSalePrice = standardSalePrice,
            failedSalePrice = failedSalePrice,
            acquisitionCostPerPole = 25_000.0,
            acquisitionStorageLocation = PoleStorageLocation.FABRICA,
        )

        @Test
        fun `isSellable returns true for terminado ok`() {
            assertTrue(testProduct(stage = ProductStage.TERMINADO, isFailed = false).isSellable())
        }

        @Test
        fun `isSellable returns true for any failed product`() {
            assertTrue(testProduct(stage = ProductStage.CRUDO, isFailed = true).isSellable())
            assertTrue(testProduct(stage = ProductStage.DESCORTEZADO, isFailed = true).isSellable())
            assertTrue(testProduct(stage = ProductStage.TRATADO, isFailed = true).isSellable())
        }

        @Test
        fun `isSellable returns false for non-terminado ok`() {
            assertFalse(testProduct(stage = ProductStage.CRUDO, isFailed = false).isSellable())
            assertFalse(testProduct(stage = ProductStage.DESCORTEZADO, isFailed = false).isSellable())
            assertFalse(testProduct(stage = ProductStage.TRATADO, isFailed = false).isSellable())
        }

        @Test
        fun `effectiveSalePrice returns standard price for ok product`() {
            val p = testProduct(standardSalePrice = 95_000.0, failedSalePrice = 35_000.0)
            assertEquals(95_000.0, p.effectiveSalePrice())
        }

        @Test
        fun `effectiveSalePrice returns failed price for failed product`() {
            val p = testProduct(isFailed = true, standardSalePrice = 95_000.0, failedSalePrice = 35_000.0)
            assertEquals(35_000.0, p.effectiveSalePrice())
        }

        @Test
        fun `effectiveSalePrice returns null when price is null`() {
            val p = testProduct(standardSalePrice = null, failedSalePrice = null)
            assertNull(p.effectiveSalePrice())
        }

        @Test
        fun `statusLabel returns OK for non-failed`() {
            assertEquals("OK", testProduct(isFailed = false).statusLabel())
        }

        @Test
        fun `statusLabel returns failed stage for failed`() {
            val p = testProduct(isFailed = true).copy(failedAtStage = ProductStage.CRUDO)
            assertEquals("Fallado en Crudo", p.statusLabel())
        }
    }

    // ── TransformationProcessingStatus ────────────────────────────────

    @Nested
    inner class TransformationProcessingStatusTests {
        @Test
        fun `fromDb handles valid values`() {
            assertEquals(
                com.inventory.industry.data.TransformationProcessingStatus.IN_PROGRESS,
                com.inventory.industry.data.TransformationProcessingStatus.fromDb("IN_PROGRESS"),
            )
            assertEquals(
                com.inventory.industry.data.TransformationProcessingStatus.COMPLETED,
                com.inventory.industry.data.TransformationProcessingStatus.fromDb("COMPLETED"),
            )
        }

        @Test
        fun `fromDb defaults to COMPLETED for unknown`() {
            assertEquals(
                com.inventory.industry.data.TransformationProcessingStatus.COMPLETED,
                com.inventory.industry.data.TransformationProcessingStatus.fromDb("UNKNOWN"),
            )
        }

        @Test
        fun `fromDb defaults to COMPLETED for null`() {
            assertEquals(
                com.inventory.industry.data.TransformationProcessingStatus.COMPLETED,
                com.inventory.industry.data.TransformationProcessingStatus.fromDb(null),
            )
        }
    }

    // ── ProviderTransportRunStatus ────────────────────────────────────

    @Nested
    inner class ProviderTransportRunStatusTests {
        @Test
        fun `fromDb handles valid values`() {
            assertEquals(
                com.inventory.industry.data.ProviderTransportRunStatus.IN_PROGRESS,
                com.inventory.industry.data.ProviderTransportRunStatus.fromDb("IN_PROGRESS"),
            )
            assertEquals(
                com.inventory.industry.data.ProviderTransportRunStatus.COMPLETED,
                com.inventory.industry.data.ProviderTransportRunStatus.fromDb("COMPLETED"),
            )
            assertEquals(
                com.inventory.industry.data.ProviderTransportRunStatus.CANCELLED,
                com.inventory.industry.data.ProviderTransportRunStatus.fromDb("CANCELLED"),
            )
        }

        @Test
        fun `fromDb defaults to IN_PROGRESS for unknown`() {
            assertEquals(
                com.inventory.industry.data.ProviderTransportRunStatus.IN_PROGRESS,
                com.inventory.industry.data.ProviderTransportRunStatus.fromDb("UNKNOWN"),
            )
        }
    }

    // ── ResourceStockLot line value ───────────────────────────────────

    @Nested
    inner class ResourceStockLotTests {
        @Test
        fun `line value estimate is quantity times price`() {
            val lot = com.inventory.industry.data.ResourceStockLot(
                id = 1, resourceId = 1, resourceName = "Test",
                resourceUnit = "kg", quantity = 100.0,
                acquisitionPricePerUnit = 5.5,
                expirationDate = null, acquiredAtEpochMs = 0L, notes = null,
            )
            assertEquals(550.0, lot.lineValueEstimate)
        }
    }

    // ── Transformation.totalInput ─────────────────────────────────────

    @Nested
    inner class TransformationTests {
        @Test
        fun `totalInput sums all input quantities`() {
            val t = com.inventory.industry.data.Transformation(
                id = 1, fromStage = ProductStage.CRUDO,
                toStage = ProductStage.DESCORTEZADO,
                processingStatus = com.inventory.industry.data.TransformationProcessingStatus.COMPLETED,
                startedAtEpochMs = 0L, processedAtEpochMs = 0L,
                durationMinutes = 60, successCount = 40.0,
                failedCount = 5.0, notes = null,
                createdAtEpochMs = 0L,
                inputs = listOf(
                    com.inventory.industry.data.TransformationInputView(
                        id = 1, sourceProductId = 1,
                        sourceName = "A", sourceLine = "L1", quantity = 25.0,
                    ),
                    com.inventory.industry.data.TransformationInputView(
                        id = 2, sourceProductId = 2,
                        sourceName = "B", sourceLine = "L2", quantity = 20.0,
                    ),
                ),
                totalCost = 0.0,
            )
            assertEquals(45.0, t.totalInput)
        }
    }
}
