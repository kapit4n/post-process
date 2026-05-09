package com.inventory.industry.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class ScreenRoute(
    val id: String,
    val title: String,
    val subtitle: String? = null,
) {
    data object Dashboard : ScreenRoute("dashboard", "Panel", "Resumen operativo")

    data object Catalog : ScreenRoute("catalog", "Catálogo", "Productos y etapas")

    data object ByStage : ScreenRoute("by_stage", "Por etapa", "Inventario por etapa")

    data object Resources : ScreenRoute("resources", "Insumos", "Materiales e insumos")

    data object Recipes : ScreenRoute("recipes", "Recetas", "Recetas por etapa")

    data object Providers : ScreenRoute("providers", "Proveedores", "Proveedores y compras")

    data object ProviderTransport :
        ScreenRoute("provider_transport", "Traslados", "Traslados entre ubicaciones")

    data object Clients : ScreenRoute("clients", "Clientes", "Cuentas y contactos")

    data object Sales : ScreenRoute("sales", "Ventas", "Pedidos y facturación")

    data object Accounting : ScreenRoute("accounting", "Contabilidad", "Movimientos contables")

    data object History : ScreenRoute("history", "Historial", "Auditoría y cambios")

    companion object {
        val mainMenu: List<ScreenRoute> =
            listOf(
                Dashboard,
                Catalog,
                ByStage,
                Resources,
                Recipes,
                Providers,
                ProviderTransport,
                Clients,
                Sales,
                Accounting,
                History,
            )

        fun fromId(id: String): ScreenRoute? = mainMenu.find { it.id == id }
    }
}

data class NavigationItem(
    val route: ScreenRoute,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String = label,
)

fun navigationItems(): List<NavigationItem> =
    listOf(
        NavigationItem(ScreenRoute.Dashboard, "Panel", Icons.Default.Dashboard),
        NavigationItem(ScreenRoute.Catalog, "Catálogo", Icons.Default.Category),
        NavigationItem(ScreenRoute.ByStage, "Por etapa", Icons.Default.Inventory),
        NavigationItem(ScreenRoute.Resources, "Insumos", Icons.Default.Science),
        NavigationItem(ScreenRoute.Recipes, "Recetas", Icons.AutoMirrored.Filled.Assignment),
        NavigationItem(ScreenRoute.Providers, "Proveedores", Icons.Default.LocalShipping),
        NavigationItem(ScreenRoute.ProviderTransport, "Traslados", Icons.Default.Navigation),
        NavigationItem(ScreenRoute.Clients, "Clientes", Icons.Default.People),
        NavigationItem(ScreenRoute.Sales, "Ventas", Icons.Default.ShoppingCart),
        NavigationItem(ScreenRoute.Accounting, "Contabilidad", Icons.Default.AccountBalance),
        NavigationItem(ScreenRoute.History, "Historial", Icons.Default.History),
    )

data class BreadcrumbSegment(
    val label: String,
    val onClick: (() -> Unit)? = null,
)
