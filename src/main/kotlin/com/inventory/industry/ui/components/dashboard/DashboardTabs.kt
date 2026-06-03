package com.inventory.industry.ui.components.dashboard

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.inventory.industry.ui.theme.AppTypography

enum class DashboardTab(val label: String) {
    Resumen("Resumen"),
    Produccion("Producción"),
    Inventario("Inventario"),
    Actividades("Actividades"),
}

@Composable
fun DashboardTabs(
    selected: DashboardTab,
    onSelected: (DashboardTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = DashboardTab.entries
    val index = tabs.indexOf(selected).coerceAtLeast(0)
    TabRow(
        selectedTabIndex = index,
        modifier = modifier.fillMaxWidth().height(40.dp),
        indicator = { positions ->
            if (positions.isNotEmpty()) {
                TabRowDefaults.SecondaryIndicator(
                    modifier =
                        Modifier
                            .tabIndicatorOffset(positions[index])
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)),
                    height = 3.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        divider = {},
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = tab == selected,
                onClick = { onSelected(tab) },
                text = {
                    Text(
                        tab.label,
                        style = AppTypography.BodySmall,
                        fontWeight =
                            if (tab == selected) {
                                androidx.compose.ui.text.font.FontWeight.SemiBold
                            } else {
                                androidx.compose.ui.text.font.FontWeight.Normal
                            },
                    )
                },
            )
        }
    }
}
