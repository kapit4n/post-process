package com.inventory.industry.ui

import com.inventory.industry.reports.PdfSaveDialog
import com.inventory.industry.ui.app.AppMessenger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Executes a PDF export workflow with standardized error handling.
 * Eliminates the duplicated try/catch blocks across DashboardScreen, ProductsByStageScreen, and SalesScreen.
 *
 * @param scope Coroutine scope to launch in
 * @param messenger Messenger for success/error feedback
 * @param defaultFileName Default filename for the save dialog
 * @param buildReport Suspend function that builds the report data
 * @param generatePdf Suspend function that generates PDF bytes from the report
 * @return true if export started, false if already in progress
 */
fun <T> exportPdfWorkflow(
    scope: CoroutineScope,
    messenger: AppMessenger,
    defaultFileName: String,
    buildReport: suspend () -> T,
    generatePdf: suspend (T) -> ByteArray,
    onStarted: () -> Unit = {},
    onFinished: () -> Unit = {},
): Boolean {
    onStarted()
    scope.launch {
        try {
            val report = withContext(Dispatchers.IO) { buildReport() }
            val bytes = withContext(Dispatchers.IO) { generatePdf(report) }
            val target = withContext(Dispatchers.Main) { PdfSaveDialog.chooseSaveFile(defaultFileName) }
            if (target != null) {
                withContext(Dispatchers.IO) { target.writeBytes(bytes) }
                messenger.showSuccess("PDF guardado: ${target.name}")
            }
        } catch (e: Exception) {
            messenger.showError("No se pudo generar el PDF: ${e.message ?: "error desconocido"}")
        } finally {
            onFinished()
        }
    }
    return true
}
