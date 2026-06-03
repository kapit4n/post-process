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

    private fun fmtQty(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else "%.2f".format(v)

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

                y = writeLine(cs, left, y, "Resumen de inventario de postes", PDType1Font.HELVETICA_BOLD, 18f)
                y -= 4f
                y = writeLine(
                    cs,
                    left,
                    y,
                    "Generado: ${generatedAt.format(generatedAtFmt)}",
                    PDType1Font.HELVETICA,
                    10f,
                )
                y -= 14f

                y = sectionTitle(cs, left, y, "Totales del sistema")
                y = writeLine(cs, left, y, "Lotes registrados: $totalLots", size = 11f)
                y = writeLine(
                    cs,
                    left,
                    y,
                    "Postes OK en proceso (Crudo / Descort. / Tratado): ${fmtQty(summary.polesInProcessOk)}",
                    size = 11f,
                )
                y = writeLine(
                    cs,
                    left,
                    y,
                    "Postes terminados listos para venta: ${fmtQty(summary.polesReadyStandardSale)}",
                    size = 11f,
                )
                y = writeLine(
                    cs,
                    left,
                    y,
                    "Postes fallados (saldo): ${fmtQty(summary.polesFailedSalvage)}",
                    size = 11f,
                )
                val grandTotal = summary.perStage.sumOf { it.totalPoles }
                val grandOk = summary.perStage.sumOf { it.okPoles }
                val grandFailed = summary.perStage.sumOf { it.failedPoles }
                y = writeLine(
                    cs,
                    left,
                    y,
                    "Suma por etapas — total: ${fmtQty(grandTotal)} · OK: ${fmtQty(grandOk)} · fallados: ${fmtQty(grandFailed)}",
                    size = 11f,
                )
                y -= 10f

                y = sectionTitle(cs, left, y, "Desglose por etapa")
                y = drawTableHeader(cs, left, y)
                summary.perStage.forEach { row ->
                    y = drawTableRow(cs, left, y, row)
                    if (y < 80f) {
                        // Sin salto de página: el reporte cabe en una hoja con 4 etapas.
                    }
                }
                y -= 6f
                cs.moveTo(left, y)
                cs.lineTo(right, y)
                cs.stroke()
                y -= 14f
                y = writeLine(
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

    private fun sectionTitle(cs: PDPageContentStream, x: Float, y: Float, title: String): Float {
        var cy = writeLine(cs, x, y, title, PDType1Font.HELVETICA_BOLD, 13f)
        cy -= 6f
        return cy
    }

    private fun drawTableHeader(cs: PDPageContentStream, x: Float, y: Float): Float {
        val cols = tableColumns(x)
        val headerY = y - 12f
        cs.setFont(PDType1Font.HELVETICA_BOLD, 10f)
        listOf("Etapa", "Lotes", "Total postes", "OK", "Fallados").forEachIndexed { i, label ->
            writeTextAt(cs, cols[i], headerY, label)
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
                "${row.stage.shortCode} — ${pdfSafe(row.stage.title)}",
                row.lotCount.toString(),
                fmtQty(row.totalPoles),
                fmtQty(row.okPoles),
                fmtQty(row.failedPoles),
            )
        cells.forEachIndexed { i, cell ->
            writeTextAt(cs, cols[i], rowY, cell)
        }
        return rowY - 14f
    }

    private fun tableColumns(startX: Float): List<Float> =
        listOf(startX, startX + 200f, startX + 260f, startX + 340f, startX + 410f)

    private fun writeLine(
        cs: PDPageContentStream,
        x: Float,
        y: Float,
        text: String,
        font: PDType1Font = PDType1Font.HELVETICA,
        size: Float = 11f,
    ): Float {
        writeTextAt(cs, x, y - size, pdfSafe(text), font, size)
        return y - size - 8f
    }

    private fun writeTextAt(
        cs: PDPageContentStream,
        x: Float,
        y: Float,
        text: String,
        font: PDType1Font = PDType1Font.HELVETICA,
        size: Float = 10f,
    ) {
        cs.beginText()
        cs.setFont(font, size)
        cs.newLineAtOffset(x, y)
        cs.showText(pdfSafe(text))
        cs.endText()
    }

    /** Helvetica WinAnsi: reemplaza caracteres fuera del rango soportado. */
    private fun pdfSafe(text: String): String =
        buildString(text.length) {
            for (ch in text) {
                when (ch.code) {
                    in 0x20..0x7E -> append(ch)
                    0xA0.toInt() -> append(' ')
                    0xE1.toInt() -> append('a') // á fallback
                    0xE9.toInt() -> append('e')
                    0xED.toInt() -> append('i')
                    0xF3.toInt() -> append('o')
                    0xFA.toInt() -> append('u')
                    0xF1.toInt() -> append('n')
                    0x2014.toInt() -> append('-')
                    else ->
                        when (ch) {
                            'á', 'à', 'ä' -> append('a')
                            'é', 'è', 'ë' -> append('e')
                            'í', 'ì', 'ï' -> append('i')
                            'ó', 'ò', 'ö' -> append('o')
                            'ú', 'ù', 'ü' -> append('u')
                            'ñ' -> append('n')
                            '—' -> append('-')
                            else -> if (ch.code < 256) append(ch) else append('?')
                        }
                }
            }
        }
}
