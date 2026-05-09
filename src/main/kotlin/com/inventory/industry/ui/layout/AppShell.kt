package com.inventory.industry.ui.layout

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
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
import com.inventory.industry.ui.app.AppMessenger
import com.inventory.industry.ui.app.LocalAppMessenger
import com.inventory.industry.ui.app.LocalSnackbarHostState
import com.inventory.industry.ui.navigation.AppSidebar
import com.inventory.industry.ui.navigation.AppTopBar
import com.inventory.industry.ui.navigation.BreadcrumbSegment
import com.inventory.industry.ui.navigation.CommandPaletteDialog
import com.inventory.industry.ui.navigation.ScreenRoute
import com.inventory.industry.ui.navigation.rememberNavigationState
import com.inventory.industry.ui.theme.AppTheme
import kotlinx.coroutines.launch

@Composable
fun AppShell(repo: InventoryRepository) {
    val navigationState = rememberNavigationState()
    var searchQuery by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val messenger =
        remember(snackbarHostState, scope) {
            AppMessenger(
                showSuccess = { msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                },
                showError = { msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                },
            )
        }
    AppTheme(darkTheme = navigationState.useDarkTheme) {
        CompositionLocalProvider(
            LocalSnackbarHostState provides snackbarHostState,
            LocalAppMessenger provides messenger,
        ) {
            BoxWithWindowSize { windowSize ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .onPreviewKeyEvent { ev ->
                            if (ev.type == KeyEventType.KeyDown && ev.key == Key.K && ev.isCtrlPressed) {
                                navigationState.toggleCommandPalette()
                                true
                            } else {
                                false
                            }
                        },
                ) {
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
                                useDarkTheme = navigationState.useDarkTheme,
                                onToggleDarkTheme = { navigationState.toggleDarkTheme() },
                                onOpenCommandPalette = { navigationState.updateCommandPaletteVisible(true) },
                            )
                        },
                        content = {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AnimatedContent(
                                    targetState = navigationState.currentRoute,
                                    modifier = Modifier.fillMaxSize(),
                                    transitionSpec = {
                                        fadeIn(animationSpec = tween(220)) togetherWith
                                            fadeOut(animationSpec = tween(180))
                                    },
                                    label = "shellRoute",
                                ) { route ->
                                    ContentContainer(windowSize = windowSize) {
                                        when (route) {
                                            ScreenRoute.Dashboard ->
                                                DashboardScreen(
                                                    repo = repo,
                                                    onNavigate = { navigationState.navigateTo(it) },
                                                )
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
                                SnackbarHost(
                                    hostState = snackbarHostState,
                                    modifier =
                                        Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(24.dp),
                                )
                            }
                        },
                    )
                    if (navigationState.commandPaletteVisible) {
                        CommandPaletteDialog(
                            navigationState = navigationState,
                            onDismiss = { navigationState.updateCommandPaletteVisible(false) },
                        )
                    }
                }
            }
        }
    }
}
