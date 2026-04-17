package com.inventory.industry.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.ShoppingCart
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
    Recipes,
    Providers,
    Clients,
    Sales,
    Accounting,
    History,
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
                NavigationRailItem(
                    selected = destination == AppDestination.Recipes,
                    onClick = { destination = AppDestination.Recipes },
                    icon = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null) },
                    label = { Text("Recetas") },
                )
                NavigationRailItem(
                    selected = destination == AppDestination.Providers,
                    onClick = { destination = AppDestination.Providers },
                    icon = { Icon(Icons.Default.LocalShipping, contentDescription = null) },
                    label = { Text("Proveedores") },
                )
                NavigationRailItem(
                    selected = destination == AppDestination.Clients,
                    onClick = { destination = AppDestination.Clients },
                    icon = { Icon(Icons.Default.People, contentDescription = null) },
                    label = { Text("Clientes") },
                )
                NavigationRailItem(
                    selected = destination == AppDestination.Sales,
                    onClick = { destination = AppDestination.Sales },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                    label = { Text("Ventas") },
                )
                NavigationRailItem(
                    selected = destination == AppDestination.Accounting,
                    onClick = { destination = AppDestination.Accounting },
                    icon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                    label = { Text("Contabilidad") },
                )
                NavigationRailItem(
                    selected = destination == AppDestination.History,
                    onClick = { destination = AppDestination.History },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("Historial") },
                )
            }
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (destination) {
                    AppDestination.Dashboard -> DashboardScreen(repo)
                    AppDestination.Catalog -> CatalogScreen(repo)
                    AppDestination.ByStage -> ProductsByStageScreen(repo)
                    AppDestination.Resources -> ResourcesScreen(repo)
                    AppDestination.Recipes -> StageRecipesScreen(repo)
                    AppDestination.Providers -> ProvidersScreen(repo)
                    AppDestination.Clients -> ClientsScreen(repo)
                    AppDestination.Sales -> SalesScreen(repo)
                    AppDestination.Accounting -> AccountingScreen(repo)
                    AppDestination.History -> HistoryScreen(repo)
                }
            }
        }
    }
}
