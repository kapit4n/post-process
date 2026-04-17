package com.inventory.industry.data

import com.inventory.industry.domain.ProductStage

data class CatalogProduct(
    val id: Int,
    val name: String,
    val productLine: String,
    val description: String?,
    val createdAtEpochMs: Long,
)

data class PoleProvider(
    val id: Int,
    val name: String,
    val contact: String?,
    val notes: String?,
    val createdAtEpochMs: Long,
)

data class Client(
    val id: Int,
    val name: String,
    val contact: String?,
    val notes: String?,
    val createdAtEpochMs: Long,
)

data class Product(
    val id: Int,
    val name: String,
    val productLine: String,
    val stage: ProductStage,
    val quantity: Double,
    val notes: String?,
    val createdAtEpochMs: Long,
    val catalogProductId: Int?,
    val providerId: Int?,
    /** Nombre del proveedor cuando el listado hace join (sólo lectura). */
    val providerName: String?,
    val isFailed: Boolean,
    val failedAtStage: ProductStage?,
    val standardSalePrice: Double?,
    val failedSalePrice: Double?,
) {
    /** Precio de venta aplicable (los fallados usan el precio de saldo). */
    fun effectiveSalePrice(): Double? =
        if (isFailed) failedSalePrice else standardSalePrice

    fun statusLabel(): String =
        if (!isFailed) {
            "OK"
        } else {
            val at = failedAtStage?.shortCode ?: "?"
            "Fallado en $at"
        }
}

/** Postes terminados sin falla, o cualquier lote fallado (saldo). */
fun Product.isSellable(): Boolean =
    (stage == ProductStage.TERMINADO && !isFailed) || isFailed

data class Resource(
    val id: Int,
    val name: String,
    val unit: String,
    val costPerUnit: Double,
)

/** Línea de receta editable: insumo esperado por poste al salir de [fromStage]. */
data class StageResourceTemplate(
    val id: Int,
    val fromStage: ProductStage,
    val resourceId: Int,
    val resourceName: String,
    val resourceUnit: String,
    val amountPerPole: Double,
    val notes: String?,
    val displayOrder: Int,
)

data class ProcessCostLine(
    val id: Int,
    val productId: Int,
    val transformationId: Int?,
    val fromStage: ProductStage,
    val toStage: ProductStage,
    val resourceId: Int?,
    val resourceName: String?,
    val amountUsed: Double?,
    val lineCost: Double,
    val label: String,
    val createdAtEpochMs: Long,
)

/** Resumen de una transformación con sus lotes fuente y costo total. */
data class Transformation(
    val id: Int,
    val fromStage: ProductStage,
    val toStage: ProductStage,
    val processedAtEpochMs: Long,
    val durationMinutes: Int,
    val successCount: Double,
    val failedCount: Double,
    val notes: String?,
    val createdAtEpochMs: Long,
    val inputs: List<TransformationInputView>,
    val totalCost: Double,
) {
    val totalInput: Double get() = successCount + failedCount
}

/** Lote fuente usado en una transformación (snapshot). */
data class TransformationInputView(
    val id: Int,
    val sourceProductId: Int?,
    val sourceName: String,
    val sourceLine: String,
    val quantity: Double,
)

/** Desglose de postes por etapa (totales y OK vs fallados). */
data class StageInventoryRow(
    val stage: ProductStage,
    val totalPoles: Double,
    val lotCount: Int,
    val okPoles: Double,
    val failedPoles: Double,
)

/** Resumen global: en proceso, listos para venta estándar, saldo fallado. */
data class InventoryFlowSummary(
    val perStage: List<StageInventoryRow>,
    /** Postes OK en Crudo / Descort. / Tratado (pueden seguir transformándose). */
    val polesInProcessOk: Double,
    /** Postes en Terminado, sin falla (venta estándar). */
    val polesReadyStandardSale: Double,
    /** Postes en lotes fallados (venta a precio de saldo). */
    val polesFailedSalvage: Double,
)

/** Línea de venta con cliente. */
data class SaleRecord(
    val id: Int,
    val productId: Int?,
    val clientId: Int,
    val clientName: String,
    val quantitySold: Double,
    val totalAmount: Double,
    val unitPrice: Double?,
    val soldAtEpochMs: Long,
    val notes: String?,
    val snapshotProductName: String,
    val snapshotProductLine: String,
    val snapshotStage: ProductStage,
    val snapshotWasFailed: Boolean,
    val snapshotProviderName: String?,
)

/** Bucket para reportes diario / mensual / anual. */
data class AccountingBucket(
    val periodKey: String,
    val displayLabel: String,
    val totalAmount: Double,
    val totalPolesSold: Double,
    val saleCount: Int,
)
