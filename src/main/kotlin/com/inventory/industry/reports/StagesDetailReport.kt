package com.inventory.industry.reports

import com.inventory.industry.data.InventoryFlowSummary
import com.inventory.industry.data.InventoryRepository
import com.inventory.industry.data.Transformation
import com.inventory.industry.domain.PoleStorageLocation
import com.inventory.industry.domain.ProductStage
import java.time.LocalDateTime

data class WipLineDetail(
    val transformationId: Int,
    val targetStageLabel: String,
    val plannedPoles: Double,
    val sourceSummary: String,
)

data class StageDetailSection(
    val stage: ProductStage,
    val lotCount: Int,
    val totalPoles: Double,
    val okPoles: Double,
    val failedPoles: Double,
    val lotsAtFactory: Int,
    val polesAtFactoryOk: Double,
    val lotsAwaitingPlant: Int,
    val polesAwaitingPlantOk: Double,
    val wipProcessCount: Int,
    val wipPolesInProcess: Double,
    val wipLines: List<WipLineDetail>,
)

data class StagesDetailReport(
    val generatedAt: LocalDateTime = LocalDateTime.now(),
    val summary: InventoryFlowSummary,
    val stages: List<StageDetailSection>,
)

fun buildStagesDetailReport(repo: InventoryRepository): StagesDetailReport {
    val summary = repo.inventoryFlowSummary()
    val products = repo.listProducts()
    val stages =
        ProductStage.entries.map { stage ->
            val stageProducts = products.filter { it.stage == stage }
            val okProducts = stageProducts.filter { !it.isFailed }
            val atFactory = okProducts.filter { it.acquisitionStorageLocation == PoleStorageLocation.FABRICA }
            val awaiting = okProducts.filter { it.acquisitionStorageLocation != PoleStorageLocation.FABRICA }
            val wip: List<Transformation> = repo.listInProgressTransformations(stage)
            val inv = summary.perStage.first { it.stage == stage }
            StageDetailSection(
                stage = stage,
                lotCount = inv.lotCount,
                totalPoles = inv.totalPoles,
                okPoles = inv.okPoles,
                failedPoles = inv.failedPoles,
                lotsAtFactory = atFactory.size,
                polesAtFactoryOk = atFactory.sumOf { it.quantity },
                lotsAwaitingPlant = awaiting.size,
                polesAwaitingPlantOk = awaiting.sumOf { it.quantity },
                wipProcessCount = wip.size,
                wipPolesInProcess = wip.sumOf { it.totalInput },
                wipLines =
                    wip.map { t ->
                        WipLineDetail(
                            transformationId = t.id,
                            targetStageLabel = t.toStage.shortCode,
                            plannedPoles = t.totalInput,
                            sourceSummary =
                                t.inputs.joinToString(", ") { inp ->
                                    "${inp.sourceName} (${PdfReportUtils.fmtQty(inp.quantity)})"
                                }.take(120),
                        )
                    },
            )
        }
    return StagesDetailReport(summary = summary, stages = stages)
}
