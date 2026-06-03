package com.inventory.industry.reports

import com.inventory.industry.data.SaleRecord
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object SalesDetailPdfGenerator {
    private val generatedAtFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val saleDateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val zone: ZoneId = ZoneId.systemDefault()
    private const val LEFT = 45f
    private const val RIGHT = 550f
    private const val BOTTOM = 70f
    private const val TOP = 780f

    fun generate(report: SalesDetailReport): ByteArray {
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

            fun ensure(minY: Float = BOTTOM + 36f) {
                if (y < minY) newPage()
            }

            y =
                PdfReportUtils.writeLine(
                    cs,
                    LEFT,
                    y,
                    "Reporte detallado de ventas",
                    PDType1Font.HELVETICA_BOLD,
                    18f,
                )
            y -= 4f
            y = PdfReportUtils.writeLine(cs, LEFT, y, "Periodo: ${PdfReportUtils.pdfSafe(report.rangeDescription)}", size = 10f)
            y =
                PdfReportUtils.writeLine(
                    cs,
                    LEFT,
                    y,
                    "Generado: ${report.generatedAt.format(generatedAtFmt)}",
                    size = 10f,
                )
            y -= 12f

            y = PdfReportUtils.sectionTitle(cs, LEFT, y, "Resumen")
            y = PdfReportUtils.writeLine(cs, LEFT, y, "Ventas registradas: ${report.saleCount}", size = 11f)
            y = PdfReportUtils.writeLine(cs, LEFT, y, "Postes vendidos: ${PdfReportUtils.fmtQty(report.totalPoles)}", size = 11f)
            y = PdfReportUtils.writeLine(cs, LEFT, y, "Total facturado: Bs ${PdfReportUtils.fmtQty(report.totalBilled)}", size = 11f)
            y = PdfReportUtils.writeLine(cs, LEFT, y, "Costo imputado: Bs ${PdfReportUtils.fmtQty(report.totalCost)}", size = 11f)
            y = PdfReportUtils.writeLine(cs, LEFT, y, "Utilidad estimada: Bs ${PdfReportUtils.fmtQty(report.totalProfit)}", size = 11f)
            y =
                PdfReportUtils.writeLine(
                    cs,
                    LEFT,
                    y,
                    "OK: ${report.okCount} · Fallados: ${report.failedCount}",
                    size = 11f,
                )
            y -= 10f

            if (report.sales.isEmpty()) {
                y = PdfReportUtils.writeLine(cs, LEFT, y, "No hay ventas en el periodo seleccionado.", size = 11f)
            } else {
                y = PdfReportUtils.sectionTitle(cs, LEFT, y, "Detalle de ventas")
                y = drawSaleTableHeader(cs, y)
                report.sales.forEach { sale ->
                    ensure(52f)
                    y = drawSaleRow(cs, y, sale)
                }
            }

            ensure()
            y =
                PdfReportUtils.writeLine(
                    cs,
                    LEFT,
                    y,
                    "Inventory Industry — reporte de ventas.",
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

    private fun formatSaleDate(epochMs: Long): String =
        Instant.ofEpochMilli(epochMs).atZone(zone).format(saleDateFmt)

    private fun drawSaleTableHeader(cs: PDPageContentStream, y: Float): Float {
        val cols = saleCols(LEFT)
        val headerY = y - 11f
        cs.setFont(PDType1Font.HELVETICA_BOLD, 8f)
        listOf("ID", "Fecha", "Cliente", "Producto", "Cant.", "Total", "Util.").forEachIndexed { i, h ->
            PdfReportUtils.writeTextAt(cs, cols[i], headerY, h, size = 8f)
        }
        val lineY = headerY - 4f
        cs.moveTo(LEFT, lineY)
        cs.lineTo(RIGHT, lineY)
        cs.stroke()
        return lineY - 8f
    }

    private fun drawSaleRow(cs: PDPageContentStream, y: Float, sale: SaleRecord): Float {
        val cols = saleCols(LEFT)
        val rowY = y - 10f
        cs.setFont(PDType1Font.HELVETICA, 8f)
        val product = PdfReportUtils.pdfSafe("${sale.snapshotProductName} (${sale.snapshotProductLine})")
        val cells =
            listOf(
                "#${sale.id}",
                formatSaleDate(sale.soldAtEpochMs),
                PdfReportUtils.pdfSafe(sale.clientName.take(18)),
                product.take(28),
                PdfReportUtils.fmtQty(sale.quantitySold),
                PdfReportUtils.fmtQty(sale.totalAmount),
                PdfReportUtils.fmtQty(sale.estimatedProfit()),
            )
        cells.forEachIndexed { i, cell ->
            PdfReportUtils.writeTextAt(cs, cols[i], rowY, cell, size = 8f)
        }
        val detailY = rowY - 10f
        PdfReportUtils.writeTextAt(
            cs,
            cols[2],
            detailY,
            PdfReportUtils.pdfSafe(
                "${sale.statusLabelForReport()} · Costo Bs ${PdfReportUtils.fmtQty(sale.estimatedTotalCost())}" +
                    (sale.notes?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
            ),
            size = 7f,
        )
        return detailY - 12f
    }

    private fun saleCols(start: Float): List<Float> =
        listOf(start, start + 28f, start + 95f, start + 155f, start + 290f, start + 325f, start + 375f)
}
