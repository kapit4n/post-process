package com.inventory.industry.ui.layout

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.inventory.industry.data.InventoryRepository
import com.inventory.industry.ui.AccountingScreen
import com.inventory.industry.ui.CatalogScreen
import com.inventory.industry.ui.ClientsScreen
import com.inventory.industry.ui.DashboardScreen
import com.inventory.industry.ui.HistoryScreen
import com.inventory.industry.ui.ProductsByStageScreen
import com.inventory.industry.ui.ProviderTransportScreen
import com.inventory.industry.ui.ProvidersScreen
import com.inventory.industry.ui.ResourcesScreen
import com.inventory.industry.ui.SalesScreen
import com.inventory.industry.ui.StageRecipesScreen
import com.inventory.industry.ui.navigation.AppSidebar
import com.inventory.industry.ui.navigation.AppTopBar
import com.inventory.industry.ui.navigation.BreadcrumbSegment
import com.inventory.industry.ui.navigation.ScreenRoute
import com.inventory.industry.ui.navigation.rememberNavigationState

@Composable
fun AppShell(repo: InventoryRepository) {
    val navigationState = rememberNavigationState()
    var searchQuery by remember { mutableStateOf("") }
    BoxWithWindowSize { windowSize ->
        AppScaffold(
            sidebar = { AppSidebar(navigationState = navigationState) },
            topBar = {
                AppTopBar(
                    title = navigationState.currentRoute.title,
                    windowSize = windowSize,
                    breadcrumbs =
                        listOf(
                            BreadcrumbSegment("Inventario"),
                            BreadcrumbSegment(navigationState.currentRoute.title),
                        ),
                    searchValue = searchQuery,
                    onSearchValueChange = { searchQuery = it },
                )
            },
            content = {
                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = navigationState.currentRoute,
                        modifier = Modifier.fillMaxSize(),
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                        },
                        label = "shellRoute",
                    ) { route ->
                        ContentContainer(windowSize = windowSize) {
                            when (route) {
                                ScreenRoute.Dashboard -> DashboardScreen(repo)
                                ScreenRoute.Catalog -> CatalogScreen(repo)
                                ScreenRoute.ByStage -> ProductsByStageScreen(repo)
                                ScreenRoute.Resources -> ResourcesScreen(repo)
                                ScreenRoute.Recipes -> StageRecipesScreen(repo)
                                ScreenRoute.Providers -> ProvidersScreen(repo)
                                ScreenRoute.ProviderTransport -> ProviderTransportScreen(repo)
                                ScreenRoute.Clients -> ClientsScreen(repo)
                                ScreenRoute.Sales -> SalesScreen(repo)
                                ScreenRoute.Accounting -> AccountingScreen(repo)
                                ScreenRoute.History -> HistoryScreen(repo)
                            }
                        }
                    }
                }
            },
        )
    }
}
