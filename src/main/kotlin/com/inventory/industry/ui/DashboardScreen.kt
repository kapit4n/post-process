package com.inventory.industry.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.inventory.industry.data.AccountingBucket
import com.inventory.industry.data.DashboardActivityEntry
import com.inventory.industry.data.InventoryFlowSummary
import com.inventory.industry.data.InventoryRepository
import com.inventory.industry.reports.PdfSaveDialog
import com.inventory.industry.reports.PolesInventoryPdfGenerator
import com.inventory.industry.ui.app.LocalAppMessenger
import com.inventory.industry.ui.components.dashboard.CompactKpiRow
import com.inventory.industry.ui.components.dashboard.DashboardActividadesTab
import com.inventory.industry.ui.components.dashboard.DashboardInventarioTab
import com.inventory.industry.ui.components.dashboard.DashboardProduccionTab
import com.inventory.industry.ui.components.dashboard.DashboardResumenTab
import com.inventory.industry.ui.components.dashboard.DashboardTab
import com.inventory.industry.ui.components.dashboard.DashboardTabs
import com.inventory.industry.ui.components.feedback.LoadingIndicator
import com.inventory.industry.ui.components.feedback.SkeletonLoader
import com.inventory.industry.ui.layout.DashboardLayout
import com.inventory.industry.ui.layout.WindowSize
import com.inventory.industry.ui.navigation.ScreenRoute
import com.inventory.industry.ui.theme.AppSpacing
import com.inventory.industry.ui.theme.AppTypography
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DashboardScreen(
    repo: InventoryRepository,
    onNavigate: (ScreenRoute) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var flow by remember { mutableStateOf<InventoryFlowSummary?>(null) }
    var productCount by remember { mutableStateOf<Int?>(null) }
    var failedCount by remember { mutableStateOf<Int?>(null) }
    var activity by remember { mutableStateOf<List<DashboardActivityEntry>?>(null) }
    var salesTrend by remember { mutableStateOf<List<AccountingBucket>?>(null) }
    var totalCost by remember { mutableStateOf<Double?>(null) }
    var inventoryValue by remember { mutableStateOf<Double?>(null) }
    var selectedTab by remember { mutableStateOf(DashboardTab.Resumen) }
    var pdfExporting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val messenger = LocalAppMessenger.current

    fun exportPdf() {
        if (pdfExporting) return
        pdfExporting = true
        scope.launch {
            try {
                val summary = withContext(Dispatchers.IO) { repo.inventoryFlowSummary() }
                val totalLots = withContext(Dispatchers.IO) { repo.listProducts().size }
                val bytes =
                    withContext(Dispatchers.IO) {
                        PolesInventoryPdfGenerator.generate(summary, totalLots)
                    }
                val defaultName =
                    "resumen-postes-${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}.pdf"
                val target =
                    withContext(Dispatchers.Main) {
                        PdfSaveDialog.chooseSaveFile(defaultName)
                    }
                if (target != null) {
                    withContext(Dispatchers.IO) { target.writeBytes(bytes) }
                    messenger.showSuccess("PDF guardado: ${target.name}")
                }
            } catch (e: Exception) {
                messenger.showError("No se pudo generar el PDF: ${e.message ?: "error desconocido"}")
            } finally {
                pdfExporting = false
            }
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            flow = repo.inventoryFlowSummary()
            val products = repo.listProducts()
            productCount = products.size
            failedCount = repo.failedProductCount()
            activity = repo.recentDashboardActivity(40)
            val today = LocalDate.now()
            salesTrend = repo.salesAggregatedDaily(today.year, today.monthValue)
            totalCost = repo.totalProcessCost()
            inventoryValue =
                products.sumOf { p ->
                    (p.acquisitionCostPerPole ?: 0.0) * p.quantity
                }
        }
    }

    val ready =
        flow != null &&
            productCount != null &&
            failedCount != null &&
            activity != null &&
            salesTrend != null &&
            totalCost != null &&
            inventoryValue != null

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val windowSize =
            remember(maxWidth, maxHeight) {
                WindowSize.fromSize(maxWidth, maxHeight)
            }
        DashboardLayout(windowSize = windowSize) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Panel de operaciones",
                    style = AppTypography.PageTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Cockpit de inventario y producción de postes.",
                    style = AppTypography.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!ready) {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    LoadingIndicator()
                    repeat(3) { SkeletonLoader(height = 64.dp) }
                }
                return@DashboardLayout
            }

            val f = flow!!
            val act = activity!!
            val trend = salesTrend!!

            CompactKpiRow(
                enProceso = formatQty(f.polesInProcessOk),
                listos = formatQty(f.polesReadyStandardSale),
                fallados = formatQty(f.polesFailedSalvage),
                lotes = productCount!!.toString(),
                valorInventario = formatMoney(inventoryValue!!),
                enProcesoBrush =
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                        ),
                    ),
                listosBrush = Brush.horizontalGradient(listOf(Color(0xFF2E7D52), Color(0xFF5BA87A))),
                falladosBrush = Brush.horizontalGradient(listOf(Color(0xFFC26D1A), Color(0xFFE8A54D))),
                lotesBrush = Brush.horizontalGradient(listOf(Color(0xFF3D6FB0), Color(0xFF6B9BD1))),
                valorBrush = Brush.horizontalGradient(listOf(Color(0xFF5E35B1), Color(0xFF9575CD))),
                enProcesoIcon = Icons.Default.Build,
                listosIcon = Icons.Outlined.CheckCircle,
                falladosIcon = Icons.Outlined.ErrorOutline,
                lotesIcon = Icons.Default.Inventory2,
                valorIcon = Icons.Default.AttachMoney,
            )

            DashboardTabs(selected = selectedTab, onSelected = { selectedTab = it })

            Crossfade(
                targetState = selectedTab,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                animationSpec = tween(220),
                label = "dashboardTab",
            ) { tab ->
                when (tab) {
                    DashboardTab.Resumen ->
                        DashboardResumenTab(
                            flow = f,
                            activity = act.take(5),
                            onNavigate = onNavigate,
                            pdfExporting = pdfExporting,
                            onExportPdf = { exportPdf() },
                            modifier = Modifier.fillMaxSize(),
                        )
                    DashboardTab.Produccion ->
                        DashboardProduccionTab(
                            flow = f,
                            salesBuckets = trend,
                            modifier = Modifier.fillMaxSize(),
                        )
                    DashboardTab.Inventario ->
                        DashboardInventarioTab(
                            flow = f,
                            totalTransformCost = totalCost!!,
                            inventoryValue = inventoryValue!!,
                            totalLots = productCount!!,
                            modifier = Modifier.fillMaxSize(),
                        )
                    DashboardTab.Actividades ->
                        DashboardActividadesTab(
                            activity = act,
                            pdfExporting = pdfExporting,
                            onExportPdf = { exportPdf() },
                            modifier = Modifier.fillMaxSize(),
                        )
                }
            }
        }
    }
}
