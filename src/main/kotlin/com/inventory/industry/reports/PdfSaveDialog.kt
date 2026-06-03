package com.inventory.industry.reports

import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

object PdfSaveDialog {
    fun chooseSaveFile(defaultFileName: String): File? {
        val chooser =
            JFileChooser().apply {
                dialogTitle = "Guardar resumen PDF"
                selectedFile = File(defaultFileName)
                fileFilter = FileNameExtensionFilter("Documento PDF (*.pdf)", "pdf")
                isAcceptAllFileFilterUsed = false
            }
        return when (chooser.showSaveDialog(null)) {
            JFileChooser.APPROVE_OPTION -> {
                val raw = chooser.selectedFile ?: return null
                if (raw.extension.equals("pdf", ignoreCase = true)) {
                    raw
                } else {
                    File(raw.parentFile, "${raw.name}.pdf")
                }
            }
            else -> null
        }
    }
}
