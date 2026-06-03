package com.inventory.industry.reports

import com.inventory.industry.domain.ProductStage
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import java.io.ByteArrayOutputStream
import java.time.format.DateTimeFormatter

object StagesDetailPdfGenerator {
    private val generatedAtFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private const val LEFT = 50f
    private const val RIGHT = 545f
    private const val BOTTOM = 72f
    private const val TOP = 780f

    fun generate(report: StagesDetailReport): ByteArray {
        PDDocument().use { doc ->
            var page = PDPage(PDRectangle.A4)
            doc.addPage(page)
            var cs = PDPageContentStream(doc, page)
            var y = TOP

            fun newPage() {
                cs.close()
                page = PDPage(PDRectangle.A4)
                doc.addPage(page)
                cs = PDPageContentStream(doc, page)
                y = TOP
            }

            fun ensure(minY: Float = BOTTOM + 40f) {
                if (y < minY) newPage()
            }

            y =
                PdfReportUtils.writeLine(
                    cs,
                    LEFT,
                    y,
                    "Reporte por etapa — postes",
                    PDType1Font.HELVETICA_BOLD,
                    18f,
                )
            y -= 4f
            y =
                PdfReportUtils.writeLine(
                    cs,
                    LEFT,
                    y,
                    "Generado: ${report.generatedAt.format(generatedAtFmt)}",
                    size = 10f,
                )
            y -= 12f

            y = PdfReportUtils.sectionTitle(cs, LEFT, y, "Resumen global")
            val s = report.summary
            y = PdfReportUtils.writeLine(cs, LEFT, y, "En proceso (OK): ${PdfReportUtils.fmtQty(s.polesInProcessOk)}", size = 11f)
            y = PdfReportUtils.writeLine(cs, LEFT, y, "Terminados listos venta: ${PdfReportUtils.fmtQty(s.polesReadyStandardSale)}", size = 11f)
            y = PdfReportUtils.writeLine(cs, LEFT, y, "Fallados / saldo: ${PdfReportUtils.fmtQty(s.polesFailedSalvage)}", size = 11f)
            y -= 8f

            y = PdfReportUtils.sectionTitle(cs, LEFT, y, "Tabla por etapa")
            y = drawSummaryTableHeader(cs, y)
            report.stages.forEach { row ->
                ensure()
                y = drawSummaryTableRow(cs, y, row)
            }
            y -= 14f

            report.stages.forEach { section ->
                ensure(120f)
                y = PdfReportUtils.sectionTitle(cs, LEFT, y, "${section.stage.shortCode} — ${PdfReportUtils.pdfSafe(section.stage.title)}")
                y =
                    PdfReportUtils.writeLine(
                        cs,
                        LEFT,
                        y,
                        "Existencia en etapa (terminado en etapa): ${section.lotCount} lote(s), " +
                            "${PdfReportUtils.fmtQty(section.totalPoles)} postes " +
                            "(OK ${PdfReportUtils.fmtQty(section.okPoles)}, fallados ${PdfReportUtils.fmtQty(section.failedPoles)})",
                        size = 10f,
                    )
                y =
                    PdfReportUtils.writeLine(
                        cs,
                        LEFT,
                        y,
                        "En planta (listos para avanzar): ${section.lotsAtFactory} lote(s), " +
                            "${PdfReportUtils.fmtQty(section.polesAtFactoryOk)} postes OK",
                        size = 10f,
                    )
                y =
                    PdfReportUtils.writeLine(
                        cs,
                        LEFT,
                        y,
                        "Pendiente recepcion en planta: ${section.lotsAwaitingPlant} lote(s), " +
                            "${PdfReportUtils.fmtQty(section.polesAwaitingPlantOk)} postes OK",
                        size = 10f,
                    )
                y =
                    PdfReportUtils.writeLine(
                        cs,
                        LEFT,
                        y,
                        "En proceso productivo: ${section.wipProcessCount} transformacion(es), " +
                            "${PdfReportUtils.fmtQty(section.wipPolesInProcess)} postes comprometidos",
                        size = 10f,
                    )
                if (section.stage == ProductStage.TERMINADO) {
                    y =
                        PdfReportUtils.writeLine(
                            cs,
                            LEFT,
                            y,
                            "Listos para venta estandar: ${PdfReportUtils.fmtQty(section.okPoles)} postes",
                            size = 10f,
                        )
                }
                if (section.wipLines.isNotEmpty()) {
                    y -= 4f
                    y = PdfReportUtils.writeLine(cs, LEFT, y, "Detalle procesos en curso:", PDType1Font.HELVETICA_BOLD, 10f)
                    section.wipLines.forEach { wip ->
                        ensure()
                        y =
                            PdfReportUtils.writeLine(
                                cs,
                                LEFT + 8f,
                                y,
                                "#${wip.transformationId} -> ${wip.targetStageLabel}: " +
                                    "${PdfReportUtils.fmtQty(wip.plannedPoles)} postes · ${PdfReportUtils.pdfSafe(wip.sourceSummary)}",
                                size = 9f,
                            )
                    }
                }
                y -= 10f
            }

            ensure()
            y =
                PdfReportUtils.writeLine(
                    cs,
                    LEFT,
                    y,
                    "Inventory Industry — reporte operativo por etapa.",
                    PDType1Font.HELVETICA_OBLIQUE,
                    9f,
                )
            cs.close()

            return ByteArrayOutputStream().use { out ->
                doc.save(out)
                out.toByteArray()
            }
        }
    }

    private fun drawSummaryTableHeader(cs: PDPageContentStream, y: Float): Float {
        val cols = summaryCols(LEFT)
        val headerY = y - 12f
        cs.setFont(PDType1Font.HELVETICA_BOLD, 9f)
        listOf("Etapa", "Lotes", "Stock", "OK", "Fall.", "En proc.", "En planta").forEachIndexed { i, label ->
            PdfReportUtils.writeTextAt(cs, cols[i], headerY, label, size = 9f)
        }
        val lineY = headerY - 5f
        cs.moveTo(LEFT, lineY)
        cs.lineTo(RIGHT, lineY)
        cs.stroke()
        return lineY - 10f
    }

    private fun drawSummaryTableRow(cs: PDPageContentStream, y: Float, row: StageDetailSection): Float {
        val cols = summaryCols(LEFT)
        val rowY = y - 11f
        cs.setFont(PDType1Font.HELVETICA, 9f)
        val cells =
            listOf(
                row.stage.shortCode,
                row.lotCount.toString(),
                PdfReportUtils.fmtQty(row.totalPoles),
                PdfReportUtils.fmtQty(row.okPoles),
                PdfReportUtils.fmtQty(row.failedPoles),
                PdfReportUtils.fmtQty(row.wipPolesInProcess),
                PdfReportUtils.fmtQty(row.polesAtFactoryOk),
            )
        cells.forEachIndexed { i, cell ->
            PdfReportUtils.writeTextAt(cs, cols[i], rowY, cell, size = 9f)
        }
        return rowY - 12f
    }

    private fun summaryCols(startX: Float): List<Float> =
        listOf(
            startX,
            startX + 52f,
            startX + 95f,
            startX + 145f,
            startX + 195f,
            startX + 245f,
            startX + 310f,
        )
}
