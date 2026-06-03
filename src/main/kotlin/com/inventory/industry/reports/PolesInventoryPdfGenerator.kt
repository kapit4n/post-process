package com.inventory.industry.reports

import com.inventory.industry.data.InventoryFlowSummary
import com.inventory.industry.data.StageInventoryRow
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object PolesInventoryPdfGenerator {
    private val generatedAtFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun generate(
        summary: InventoryFlowSummary,
        totalLots: Int,
        generatedAt: LocalDateTime = LocalDateTime.now(),
    ): ByteArray {
        PDDocument().use { doc ->
            val page = PDPage(PDRectangle.A4)
            doc.addPage(page)
            PDPageContentStream(doc, page).use { cs ->
                var y = 780f
                val left = 50f
                val right = 545f

                y = PdfReportUtils.writeLine(cs, left, y, "Resumen de inventario de postes", PDType1Font.HELVETICA_BOLD, 18f)
                y -= 4f
                y =
                    PdfReportUtils.writeLine(
                        cs,
                        left,
                        y,
                        "Generado: ${generatedAt.format(generatedAtFmt)}",
                        size = 10f,
                    )
                y -= 14f

                y = PdfReportUtils.sectionTitle(cs, left, y, "Totales del sistema")
                y = PdfReportUtils.writeLine(cs, left, y, "Lotes registrados: $totalLots", size = 11f)
                y =
                    PdfReportUtils.writeLine(
                        cs,
                        left,
                        y,
                        "Postes OK en proceso (Crudo / Descort. / Tratado): ${PdfReportUtils.fmtQty(summary.polesInProcessOk)}",
                        size = 11f,
                    )
                y =
                    PdfReportUtils.writeLine(
                        cs,
                        left,
                        y,
                        "Postes terminados listos para venta: ${PdfReportUtils.fmtQty(summary.polesReadyStandardSale)}",
                        size = 11f,
                    )
                y =
                    PdfReportUtils.writeLine(
                        cs,
                        left,
                        y,
                        "Postes fallados (saldo): ${PdfReportUtils.fmtQty(summary.polesFailedSalvage)}",
                        size = 11f,
                    )
                val grandTotal = summary.perStage.sumOf { it.totalPoles }
                val grandOk = summary.perStage.sumOf { it.okPoles }
                val grandFailed = summary.perStage.sumOf { it.failedPoles }
                y =
                    PdfReportUtils.writeLine(
                        cs,
                        left,
                        y,
                        "Suma por etapas — total: ${PdfReportUtils.fmtQty(grandTotal)} · OK: ${PdfReportUtils.fmtQty(grandOk)} · fallados: ${PdfReportUtils.fmtQty(grandFailed)}",
                        size = 11f,
                    )
                y -= 10f

                y = PdfReportUtils.sectionTitle(cs, left, y, "Desglose por etapa")
                y = drawTableHeader(cs, left, y)
                summary.perStage.forEach { row ->
                    y = drawTableRow(cs, left, y, row)
                }
                y -= 6f
                cs.moveTo(left, y)
                cs.lineTo(right, y)
                cs.stroke()
                y -= 14f
                y =
                    PdfReportUtils.writeLine(
                        cs,
                        left,
                        y,
                        "Inventory Industry — reporte operativo de postes de madera.",
                        PDType1Font.HELVETICA_OBLIQUE,
                        9f,
                    )
            }
            return ByteArrayOutputStream().use { out ->
                doc.save(out)
                out.toByteArray()
            }
        }
    }

    private fun drawTableHeader(cs: PDPageContentStream, x: Float, y: Float): Float {
        val cols = tableColumns(x)
        val headerY = y - 12f
        cs.setFont(PDType1Font.HELVETICA_BOLD, 10f)
        listOf("Etapa", "Lotes", "Total postes", "OK", "Fallados").forEachIndexed { i, label ->
            PdfReportUtils.writeTextAt(cs, cols[i], headerY, label)
        }
        val lineY = headerY - 6f
        cs.moveTo(x, lineY)
        cs.lineTo(545f, lineY)
        cs.stroke()
        return lineY - 10f
    }

    private fun drawTableRow(cs: PDPageContentStream, x: Float, y: Float, row: StageInventoryRow): Float {
        val cols = tableColumns(x)
        val rowY = y - 12f
        cs.setFont(PDType1Font.HELVETICA, 10f)
        val cells =
            listOf(
                "${row.stage.shortCode} — ${PdfReportUtils.pdfSafe(row.stage.title)}",
                row.lotCount.toString(),
                PdfReportUtils.fmtQty(row.totalPoles),
                PdfReportUtils.fmtQty(row.okPoles),
                PdfReportUtils.fmtQty(row.failedPoles),
            )
        cells.forEachIndexed { i, cell ->
            PdfReportUtils.writeTextAt(cs, cols[i], rowY, cell)
        }
        return rowY - 14f
    }

    private fun tableColumns(startX: Float): List<Float> =
        listOf(startX, startX + 200f, startX + 260f, startX + 340f, startX + 410f)
}
