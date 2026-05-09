package com.inventory.industry.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inventory.industry.data.AccountingBucket
import com.inventory.industry.data.DashboardActivityEntry
import com.inventory.industry.data.DashboardActivityKind
import com.inventory.industry.data.InventoryFlowSummary
import com.inventory.industry.data.InventoryRepository
import com.inventory.industry.data.StageInventoryRow
import com.inventory.industry.ui.charts.BarChartEntry
import com.inventory.industry.ui.charts.DonutSegment
import com.inventory.industry.ui.charts.EnterpriseBarChart
import com.inventory.industry.ui.charts.EnterpriseDonutChart
import com.inventory.industry.ui.charts.EnterpriseLineChart
import com.inventory.industry.ui.charts.LinePoint
import com.inventory.industry.ui.components.buttons.AppButton
import com.inventory.industry.ui.components.buttons.AppOutlinedButton
import com.inventory.industry.ui.components.cards.AppCard
import com.inventory.industry.ui.components.cards.SectionCard
import com.inventory.industry.ui.components.feedback.LoadingIndicator
import com.inventory.industry.ui.components.feedback.SkeletonLoader
import com.inventory.industry.ui.layout.DashboardLayout
import com.inventory.industry.ui.layout.WindowSize
import com.inventory.industry.ui.navigation.ScreenRoute
import com.inventory.industry.ui.theme.AppShapes
import com.inventory.industry.ui.theme.AppSpacing
import com.inventory.industry.ui.theme.AppTypography
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
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

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            flow = repo.inventoryFlowSummary()
            productCount = repo.listProducts().size
            failedCount = repo.failedProductCount()
            activity = repo.recentDashboardActivity(18)
            val today = LocalDate.now()
            salesTrend = repo.salesAggregatedDaily(today.year, today.monthValue)
            totalCost = repo.totalProcessCost()
        }
    }

    val ready = flow != null && productCount != null && failedCount != null && activity != null && salesTrend != null && totalCost != null

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val windowSize =
            remember(maxWidth, maxHeight) {
                WindowSize.fromSize(maxWidth, maxHeight)
            }
        DashboardLayout(windowSize = windowSize) {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Text(
                    text = "Resumen",
                    style = AppTypography.PageTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text =
                        "Panel analítico del flujo de postes: materia prima, etapas, inventario listo y salidas.",
                    style = AppTypography.Body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!ready) {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                    LoadingIndicator()
                    repeat(4) { SkeletonLoader(height = 72.dp) }
                }
                return@DashboardLayout
            }

            val f = flow!!
            val act = activity!!
            val trend = salesTrend!!

            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                KpiRow(
                    flow = f,
                    lotsTotal = productCount!!,
                    failedLots = failedCount!!,
                    totalTransformCost = totalCost!!,
                )
                StageAnalyticsSection(rows = f.perStage)
                ChartsSection(flow = f, salesBuckets = trend)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.lg),
                    verticalAlignment = Alignment.Top,
                ) {
                    SectionCard(
                        title = "Actividad reciente",
                        subtitle = "Ventas, producción y traslados",
                        modifier = Modifier.weight(1.2f),
                    ) {
                        ActivityFeed(entries = act)
                    }
                    QuickActionsColumn(
                        modifier = Modifier.weight(0.85f),
                        onNavigate = onNavigate,
                    )
                }
                InventorySummaryCard(flow = f, totalCost = totalCost!!)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KpiRow(
    flow: InventoryFlowSummary,
    lotsTotal: Int,
    failedLots: Int,
    totalTransformCost: Double,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
        maxItemsInEachRow = 4,
    ) {
        DashboardKpiCard(
            title = "Postes OK en proceso",
            value = formatQty(flow.polesInProcessOk),
            subtitle = "Crudo · Descortezado · Tratado",
            trendLabel = "En transformación",
            trendPositive = true,
            icon = Icons.Default.Build,
            accent =
                Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                    ),
                ),
            modifier = Modifier.widthIn(min = 200.dp, max = 280.dp),
        )
        DashboardKpiCard(
            title = "Listos para venta",
            value = formatQty(flow.polesReadyStandardSale),
            subtitle = "Terminado sin falla",
            trendLabel = "Stock estándar",
            trendPositive = true,
            icon = Icons.Outlined.CheckCircle,
            accent =
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF2E7D52),
                        Color(0xFF5BA87A),
                    ),
                ),
            modifier = Modifier.widthIn(min = 200.dp, max = 280.dp),
        )
        DashboardKpiCard(
            title = "Stock fallado",
            value = formatQty(flow.polesFailedSalvage),
            subtitle = "$failedLots lote(s) marcados",
            trendLabel = if (flow.polesFailedSalvage > 0) "Revisar saldo" else "Sin saldo",
            trendPositive = flow.polesFailedSalvage <= 0,
            icon = Icons.Outlined.ErrorOutline,
            accent =
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFFC26D1A),
                        Color(0xFFE8A54D),
                    ),
                ),
            modifier = Modifier.widthIn(min = 200.dp, max = 280.dp),
        )
        DashboardKpiCard(
            title = "Lotes registrados",
            value = lotsTotal.toString(),
            subtitle = "Inversión proceso: ${formatMoney(totalTransformCost)}",
            trendLabel = "Inventario activo",
            trendPositive = true,
            icon = Icons.Default.Inventory2,
            accent =
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF3D6FB0),
                        Color(0xFF6B9BD1),
                    ),
                ),
            modifier = Modifier.widthIn(min = 200.dp, max = 280.dp),
        )
    }
}

@Composable
private fun DashboardKpiCard(
    title: String,
    value: String,
    subtitle: String,
    trendLabel: String,
    trendPositive: Boolean,
    icon: ImageVector,
    accent: Brush,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier, enableHoverElevation = true) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(AppShapes.medium)
                        .background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = AppTypography.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = AppTypography.MetricMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = AppTypography.BodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = if (trendPositive) Icons.Default.TrendingUp else Icons.Default.TrendingFlat,
                        contentDescription = null,
                        tint =
                            if (trendPositive) Color(0xFF2E7D52) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        trendLabel,
                        style = AppTypography.Caption,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun StageAnalyticsSection(rows: List<StageInventoryRow>) {
    SectionCard(title = "Avance por etapa", subtitle = "OK vs fallados y lotes activos") {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
            rows.forEach { row ->
                StageProgressCard(row = row)
            }
        }
    }
}

@Composable
private fun StageProgressCard(row: StageInventoryRow) {
    val total = row.okPoles + row.failedPoles
    val pct =
        if (total > 1e-6) {
            (row.okPoles / total).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }
    val animated by animateFloatAsState(targetValue = pct, animationSpec = tween(500), label = "stagePct")
    AppCard(enableHoverElevation = false) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column {
                        Text(
                            "${row.stage.shortCode} — ${row.stage.title}",
                            style = AppTypography.CardTitle,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "${row.lotCount} lote(s) · ${formatQty(row.totalPoles)} postes",
                            style = AppTypography.Caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    "${(animated * 100).toInt()}% OK",
                    style = AppTypography.MetricMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            LinearProgressIndicator(
                progress = { animated },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(AppShapes.small),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.lg)) {
                Text(
                    "OK: ${formatQty(row.okPoles)}",
                    style = AppTypography.BodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Fallados: ${formatQty(row.failedPoles)}",
                    style = AppTypography.BodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ChartsSection(
    flow: InventoryFlowSummary,
    salesBuckets: List<AccountingBucket>,
) {
    SectionCard(title = "Analítica", subtitle = "Distribución y tendencia de ventas del mes") {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)) {
            val barEntries =
                flow.perStage.mapIndexed { i, r ->
                    val hue =
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            Color(0xFF5C6BC0),
                            Color(0xFF26A69A),
                            Color(0xFFFFB74D),
                        )[i % 4]
                    BarChartEntry(r.stage.shortCode, r.totalPoles.toFloat(), hue)
                }
            EnterpriseBarChart(title = "Postes por etapa", entries = barEntries, chartHeight = 180.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.lg),
            ) {
                val donut =
                    listOf(
                        DonutSegment("OK proceso", flow.polesInProcessOk.toFloat(), MaterialTheme.colorScheme.primary),
                        DonutSegment("Listos", flow.polesReadyStandardSale.toFloat(), Color(0xFF43A047)),
                        DonutSegment("Saldo", flow.polesFailedSalvage.toFloat(), Color(0xFFE65100)),
                    )
                EnterpriseDonutChart(
                    title = "Estado del inventario",
                    segments = donut,
                    modifier = Modifier.weight(1f),
                    chartDiameter = 200.dp,
                )
                val linePoints =
                    salesBuckets.mapIndexed { i, b ->
                        LinePoint(i.toString(), b.totalPolesSold.toFloat().coerceAtLeast(0f))
                    }
                if (linePoints.isNotEmpty()) {
                    EnterpriseLineChart(
                        title = "Ventas (postes / día, mes actual)",
                        points = linePoints,
                        modifier = Modifier.weight(1.2f),
                        lineColor = Color(0xFF3949AB),
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityFeed(entries: List<DashboardActivityEntry>) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        entries.forEach { e ->
            ActivityRow(entry = e)
        }
    }
}

@Composable
private fun ActivityRow(entry: DashboardActivityEntry) {
    val (icon, tint) =
        when (entry.kind) {
            DashboardActivityKind.Sale -> Icons.Outlined.ShoppingCart to Color(0xFF2E7D52)
            DashboardActivityKind.Transformation -> Icons.Default.ShowChart to MaterialTheme.colorScheme.primary
            DashboardActivityKind.Transport -> Icons.Default.LocalShipping to Color(0xFF1565C0)
            DashboardActivityKind.Inventory -> Icons.Outlined.History to MaterialTheme.colorScheme.tertiary
        }
    AppCard(
        enableHoverElevation = true,
        contentPadding = PaddingValues(AppSpacing.md),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(entry.title, style = AppTypography.Body, color = MaterialTheme.colorScheme.onSurface)
                Text(entry.subtitle, style = AppTypography.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    formatEpochMs(entry.epochMs),
                    style = AppTypography.Caption,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun QuickActionsColumn(
    onNavigate: (ScreenRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(title = "Acciones rápidas", subtitle = "Atajos operativos", modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            AppButton(
                text = "Nuevo lote",
                onClick = { onNavigate(ScreenRoute.ByStage) },
                modifier = Modifier.fillMaxWidth(),
            )
            AppOutlinedButton(
                text = "Registrar venta",
                onClick = { onNavigate(ScreenRoute.Sales) },
                modifier = Modifier.fillMaxWidth(),
            )
            AppOutlinedButton(
                text = "Agregar producto (catálogo)",
                onClick = { onNavigate(ScreenRoute.Catalog) },
                modifier = Modifier.fillMaxWidth(),
            )
            AppOutlinedButton(
                text = "Crear / editar receta",
                onClick = { onNavigate(ScreenRoute.Recipes) },
                modifier = Modifier.fillMaxWidth(),
            )
            AppOutlinedButton(
                text = "Registrar proveedor",
                onClick = { onNavigate(ScreenRoute.Providers) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun InventorySummaryCard(
    flow: InventoryFlowSummary,
    totalCost: Double,
) {
    SectionCard(title = "Resumen de inventario", subtitle = "Costos de transformación acumulados") {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            Text(
                "Gasto en insumos de transformación: ${formatMoney(totalCost)}",
                style = AppTypography.Body,
                color = MaterialTheme.colorScheme.onSurface,
            )
            HorizontalDivider()
            flow.perStage.forEach { r ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("${r.stage.shortCode} · ${r.lotCount} lotes", style = AppTypography.BodySmall)
                    Text(formatQty(r.totalPoles), style = AppTypography.BodySmall, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
