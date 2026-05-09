package com.inventory.industry.data

import com.inventory.industry.domain.PoleStorageLocation
import com.inventory.industry.domain.ProductStage
import java.time.LocalDate

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

data class Driver(
    val id: Int,
    val name: String,
    val phone: String?,
    val notes: String?,
    val createdAtEpochMs: Long,
)

enum class ProviderTransportRunStatus {
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    ;

    companion object {
        fun fromDb(value: String): ProviderTransportRunStatus =
            entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
                ?: IN_PROGRESS
    }
}

data class ProviderTransportRunLot(
    val productId: Int,
    val productName: String,
    val quantity: Double,
)

data class ProviderTransportRun(
    val id: Int,
    val driverId: Int,
    val driverName: String,
    val vehiclePlate: String,
    val freightCost: Double,
    val gruaCost: Double,
    val departedAtEpochMs: Long,
    val expectedArrivalEpochMs: Long?,
    val arrivedAtEpochMs: Long?,
    val status: ProviderTransportRunStatus,
    val notes: String?,
    val createdAtEpochMs: Long,
    val lots: List<ProviderTransportRunLot>,
)

/** Datos del envío activo cuando un lote está [PoleStorageLocation.EN_TRANSITO]. */
data class PoleInboundEta(
    val transportRunId: Int,
    val driverName: String,
    val vehiclePlate: String,
    val departedAtEpochMs: Long,
    val expectedArrivalEpochMs: Long?,
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
    /** Costo de adquisición por poste (materia prima), si se registró. */
    val acquisitionCostPerPole: Double?,
    /** Ubicación física del lote respecto a planta: define desde dónde se cargan traslados y cómo avanzar a Fábrica vía «Traslados». */
    val acquisitionStorageLocation: PoleStorageLocation,
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

/** Línea editable de traslado al guardar un lote. */
data class AcquisitionTransportLineDraft(
    val label: String,
    val lineCost: Double,
    val notes: String? = null,
)

/** Línea persistida de costo de traslado (camión, cargador…). */
data class AcquisitionTransportLine(
    val id: Int,
    val productId: Int?,
    val label: String,
    val lineCost: Double,
    val notes: String?,
    val createdAtEpochMs: Long,
)

data class Resource(
    val id: Int,
    val name: String,
    val unit: String,
    val costPerUnit: Double,
)

/** Partida de stock de un insumo (compra / lote con vencimiento). */
data class ResourceStockLot(
    val id: Int,
    val resourceId: Int,
    val resourceName: String,
    val resourceUnit: String,
    val quantity: Double,
    val acquisitionPricePerUnit: Double,
    val expirationDate: LocalDate?,
    val acquiredAtEpochMs: Long,
    val notes: String?,
) {
    val lineValueEstimate: Double get() = quantity * acquisitionPricePerUnit
}

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
    /** Null si el lote se eliminó pero se conserva la línea para contabilidad. */
    val productId: Int?,
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

/** Estado del flujo: proceso declarado vs ya cerrado en inventario. */
enum class TransformationProcessingStatus {
    IN_PROGRESS,
    COMPLETED,
    ;

    companion object {
        fun fromDb(value: String?): TransformationProcessingStatus =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: COMPLETED
    }
}

/** Resumen de una transformación con sus lotes fuente y costo total. */
data class Transformation(
    val id: Int,
    val fromStage: ProductStage,
    val toStage: ProductStage,
    val processingStatus: TransformationProcessingStatus,
    /** Inicio del proceso intermedio (entre etapas); null en registros antiguos. */
    val startedAtEpochMs: Long?,
    val processedAtEpochMs: Long,
    val durationMinutes: Int,
    val successCount: Double,
    val failedCount: Double,
    val notes: String?,
    val createdAtEpochMs: Long,
    val inputs: List<TransformationInputView>,
    val totalCost: Double,
) {
    /** Total de postes planeados o tomados de los lotes fuente (siempre coincide con las líneas de entrada). */
    val totalInput: Double get() = inputs.sumOf { it.quantity }
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
    val snapshotAcquisitionCostTotal: Double,
    val snapshotAcquisitionMaterialTotal: Double,
    val snapshotAcquisitionTransportTotal: Double,
    val snapshotProcessingCostTotal: Double,
    val snapshotUnitCostBasis: Double?,
    val snapshotMarginPercent: Double?,
    val snapshotSuggestedTotal: Double?,
)

/** Vista previa de costos para una venta (antes de confirmar). */
data class SaleCostPreview(
    val quantityAvailable: Double,
    /** Solo materia prima / precio al proveedor por poste. */
    val acquisitionMaterialPerPole: Double?,
    /** Traslado prorrateado por poste (total traslado del lote ÷ cantidad en lote). */
    val acquisitionTransportPerPole: Double,
    /** Materia prima + traslado por poste (antes de proceso). */
    val landedAcquisitionPerPole: Double,
    val processingCostTotalOnLot: Double,
    val processingCostPerPole: Double,
    val unitCostBasis: Double,
    val acquisitionMaterialTotalForSaleQty: Double,
    val acquisitionTransportTotalForSaleQty: Double,
    val acquisitionTotalForSaleQty: Double,
    val processingTotalForSaleQty: Double,
    /** Por poste, con margen: `(material + traslado prorrateado + proceso) × (1 + margen %)`. */
    val suggestedUnitPrice: Double,
    /** Cantidad vendida × precio sugerido por poste; la base del costo incluye traslado prorrateado. */
    val suggestedTotal: Double,
)

/** Resumen de costos para la pantalla de contabilidad. */
data class AccountingCostOverview(
    /** Suma de todas las líneas de insumos registradas (incluye lotes ya vendidos / borrados). */
    val totalProcessingCostAllTime: Double,
    /** Líneas aún vinculadas a un lote en inventario. */
    val processingCostAttributedToOpenStock: Double,
    /** Suma de cantidad × costo de adquisición en lotes vivos (costo nulo → 0). */
    val inventoryAcquisitionCostTotal: Double,
    /** Costo de adquisición imputado en ventas registradas (snapshots). */
    val soldAcquisitionCostTotal: Double,
    /** Costo de procesamiento imputado en ventas registradas (snapshots). */
    val soldProcessingCostTotal: Double,
    /** Traslado registrado (histórico, todas las líneas). */
    val totalAcquisitionTransportAllTime: Double,
    /** Traslado vinculado a lotes aún en inventario. */
    val acquisitionTransportAttributedToOpenStock: Double,
    /** Traslado imputado en ventas (snapshots). */
    val soldAcquisitionTransportTotal: Double,
)

/** Bucket para reportes diario / mensual / anual. */
data class AccountingBucket(
    val periodKey: String,
    val displayLabel: String,
    val totalAmount: Double,
    val totalPolesSold: Double,
    val saleCount: Int,
)
