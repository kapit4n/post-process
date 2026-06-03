package com.inventory.industry.reports

import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font

internal object PdfReportUtils {
    fun fmtQty(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else "%.2f".format(v)

    fun writeLine(
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

    fun writeTextAt(
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

    fun sectionTitle(cs: PDPageContentStream, x: Float, y: Float, title: String): Float {
        var cy = writeLine(cs, x, y, title, PDType1Font.HELVETICA_BOLD, 13f)
        cy -= 6f
        return cy
    }

    fun pdfSafe(text: String): String =
        buildString(text.length) {
            for (ch in text) {
                when (ch.code) {
                    in 0x20..0x7E -> append(ch)
                    0xA0.toInt() -> append(' ')
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
