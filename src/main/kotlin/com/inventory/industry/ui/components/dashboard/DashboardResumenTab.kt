package com.inventory.industry.ui.components.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inventory.industry.data.DashboardActivityEntry
import com.inventory.industry.data.InventoryFlowSummary
import com.inventory.industry.ui.navigation.ScreenRoute
import com.inventory.industry.ui.theme.AppSpacing

@Composable
fun DashboardResumenTab(
    flow: InventoryFlowSummary,
    activity: List<DashboardActivityEntry>,
    onNavigate: (ScreenRoute) -> Unit,
    pdfExporting: Boolean,
    onExportPdf: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val stacked = maxWidth < 960.dp
        if (stacked) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                CompactStageProgressCard(
                    rows = flow.perStage,
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                )
                InventoryDonutCard(flow = flow, modifier = Modifier.fillMaxWidth())
                QuickAnalyticsCard(flow = flow, modifier = Modifier.fillMaxWidth())
                RecentActivityCard(entries = activity, modifier = Modifier.fillMaxWidth().weight(1f, fill = false))
                QuickActionsGrid(
                    onNavigate = onNavigate,
                    pdfExporting = pdfExporting,
                    onExportPdf = onExportPdf,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                ) {
                    CompactStageProgressCard(
                        rows = flow.perStage,
                        modifier = Modifier.weight(1.05f).fillMaxSize(),
                    )
                    Column(
                        modifier = Modifier.weight(0.95f).fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    ) {
                        InventoryDonutCard(
                            flow = flow,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )
                        QuickAnalyticsCard(
                            flow = flow,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )
                    }
                }
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                ) {
                    RecentActivityCard(
                        entries = activity,
                        modifier = Modifier.weight(1.15f).fillMaxSize(),
                    )
                    QuickActionsGrid(
                        onNavigate = onNavigate,
                        pdfExporting = pdfExporting,
                        onExportPdf = onExportPdf,
                        modifier = Modifier.weight(0.85f).fillMaxSize(),
                    )
                }
            }
        }
    }
}
