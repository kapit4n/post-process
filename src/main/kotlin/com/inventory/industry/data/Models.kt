package com.inventory.industry.data

import com.inventory.industry.domain.ProductStage

data class CatalogProduct(
    val id: Int,
    val name: String,
    val productLine: String,
    val description: String?,
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

data class Resource(
    val id: Int,
    val name: String,
    val unit: String,
    val costPerUnit: Double,
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
