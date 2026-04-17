package com.inventory.industry.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inventory.industry.data.InventoryRepository

private enum class AppDestination {
    Dashboard,
    Catalog,
    ByStage,
    Resources,
}

@Composable
fun AppShell(repo: InventoryRepository) {
    var destination by remember { mutableStateOf(AppDestination.Dashboard) }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail {
                NavigationRailItem(
                    selected = destination == AppDestination.Dashboard,
                    onClick = { destination = AppDestination.Dashboard },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("Panel") },
                )
                NavigationRailItem(
                    selected = destination == AppDestination.Catalog,
                    onClick = { destination = AppDestination.Catalog },
                    icon = { Icon(Icons.Default.Category, contentDescription = null) },
                    label = { Text("Catálogo") },
                )
                NavigationRailItem(
                    selected = destination == AppDestination.ByStage,
                    onClick = { destination = AppDestination.ByStage },
                    icon = { Icon(Icons.Default.Inventory, contentDescription = null) },
                    label = { Text("Por etapa") },
                )
                NavigationRailItem(
                    selected = destination == AppDestination.Resources,
                    onClick = { destination = AppDestination.Resources },
                    icon = { Icon(Icons.Default.Science, contentDescription = null) },
                    label = { Text("Insumos") },
                )
            }
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (destination) {
                    AppDestination.Dashboard -> DashboardScreen(repo)
                    AppDestination.Catalog -> CatalogScreen(repo)
                    AppDestination.ByStage -> ProductsByStageScreen(repo)
                    AppDestination.Resources -> ResourcesScreen(repo)
                }
            }
        }
    }
}
