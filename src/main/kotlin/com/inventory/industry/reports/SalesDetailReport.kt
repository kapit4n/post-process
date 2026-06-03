package com.inventory.industry.reports

import com.inventory.industry.data.InventoryRepository
import com.inventory.industry.data.SaleRecord
import com.inventory.industry.domain.ProductStage
import java.time.LocalDateTime

data class SalesDetailReport(
    val rangeDescription: String,
    val generatedAt: LocalDateTime = LocalDateTime.now(),
    val sales: List<SaleRecord>,
) {
    val saleCount: Int get() = sales.size
    val totalBilled: Double get() = sales.sumOf { it.totalAmount }
    val totalPoles: Double get() = sales.sumOf { it.quantitySold }
    val totalCost: Double get() = sales.sumOf { it.snapshotAcquisitionCostTotal + it.snapshotProcessingCostTotal }
    val totalProfit: Double get() = totalBilled - totalCost
    val okCount: Int get() = sales.count { !it.snapshotWasFailed && it.snapshotStage == ProductStage.TERMINADO }
    val failedCount: Int get() = sales.count { it.snapshotWasFailed }
}

fun buildSalesDetailReport(
    repo: InventoryRepository,
    epochRange: SalesEpochRange,
): SalesDetailReport {
    val sales =
        if (epochRange.fromEpochMs == null && epochRange.toEpochMsExclusive == null) {
            repo.loadAllSales()
        } else {
            repo.listSalesInRange(epochRange.fromEpochMs, epochRange.toEpochMsExclusive)
        }
    return SalesDetailReport(
        rangeDescription = epochRange.description,
        sales = sales,
    )
}

fun SaleRecord.estimatedTotalCost(): Double =
    snapshotAcquisitionCostTotal + snapshotProcessingCostTotal

fun SaleRecord.estimatedProfit(): Double = totalAmount - estimatedTotalCost()

fun SaleRecord.statusLabelForReport(): String =
    when {
        notes?.contains("cancel", ignoreCase = true) == true -> "Cancelada"
        snapshotWasFailed -> "Fallado"
        snapshotStage == ProductStage.TERMINADO -> "OK"
        else -> "Pendiente"
    }
